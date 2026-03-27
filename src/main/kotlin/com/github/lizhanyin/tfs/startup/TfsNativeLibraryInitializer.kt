package com.github.lizhanyin.tfs.startup

import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * TFS 本地库初始化器
 * 负责从 JAR 中提取并加载 TFS SDK 所需的本地库
 */
object TfsNativeLibraryInitializer {

    private const val NATIVE_BASE_DIR_PROPERTY = "com.microsoft.tfs.jni.native.base-directory"
    private val LOG = Logger.getInstance(TfsNativeLibraryInitializer::class.java)
    private var initialized = false
    private var tempDir: Path? = null

    /**
     * 初始化 TFS 本地库
     */
    @Synchronized
    fun init() {
        if (initialized) {
            return
        }

        // 如果已经设置了本地库路径，跳过
        if (System.getProperty(NATIVE_BASE_DIR_PROPERTY) != null) {
            LOG.info("TFS native library path already set: ${System.getProperty(NATIVE_BASE_DIR_PROPERTY)}")
            initialized = true
            return
        }

        try {
            // 获取操作系统和架构信息
            val osName = System.getProperty("os.name").lowercase()
            val osArch = System.getProperty("os.arch").lowercase()

            LOG.info("TFS: Detected platform: $osName $osArch")

            val platformPath = getPlatformPath(osName, osArch)
            if (platformPath == null) {
                LOG.warn("TFS: Unsupported platform: $osName $osArch")
                return
            }

            LOG.info("TFS: Platform path: $platformPath")

            // 创建临时目录来存放本地库
            val nativeTempDir = Files.createTempDirectory("tfs-native-")
            tempDir = nativeTempDir
            LOG.info("TFS: Temp directory: $nativeTempDir")

            // 提取并加载本地库文件
            extractAndLoadNativeLibraries(nativeTempDir, platformPath)

            // 设置系统属性
            System.setProperty(NATIVE_BASE_DIR_PROPERTY, nativeTempDir.toString())

            // Windows 上需要将 DLL 目录添加到 PATH
            if (osName.contains("win")) {
                addToWindowsPath(nativeTempDir)
            }

            LOG.info("TFS: Native libraries initialized successfully")

            // 添加 JVM 退出时的清理钩子
            Runtime.getRuntime().addShutdownHook(Thread {
                try {
                    tempDir?.let { deleteDirectory(it) }
                } catch (e: IOException) {
                    // 忽略清理错误
                }
            })

            initialized = true
        } catch (e: Exception) {
            LOG.error("TFS: Failed to initialize native libraries", e)
            e.printStackTrace()
        }
    }

    /**
     * 根据操作系统和架构获取本地库路径
     */
    private fun getPlatformPath(osName: String, osArch: String): String? {
        val platform = when {
            osName.contains("win") -> "win32"
            osName.contains("linux") -> "linux"
            osName.contains("mac") -> "macosx"
            osName.contains("sunos") || osName.contains("solaris") -> "solaris"
            osName.contains("aix") -> "aix"
            osName.contains("hp-ux") || osName.contains("hpux") -> "hpux"
            osName.contains("freebsd") -> "freebsd"
            else -> return null
        }

        val arch = when {
            osArch.contains("64") -> "x86_64"
            osArch.contains("86") -> "x86"
            osArch.contains("arm") -> "arm"
            osArch.contains("ppc") || osArch.contains("power") -> "ppc"
            osArch.contains("sparc") -> "sparc"
            osArch.contains("ia64") -> "ia64_32"
            else -> "x86"
        }

        // macOS 特殊处理
        return if (platform == "macosx") {
            "native/macosx"
        } else {
            "native/$platform/$arch"
        }
    }

    /**
     * 从 JAR 中提取并加载本地库
     */
    private fun extractAndLoadNativeLibraries(targetDir: Path, platformPath: String) {
        val libraryNames = getLibraryNamesForPlatform(platformPath)

        for (libName in libraryNames) {
            val resourcePath = "$platformPath/$libName"
            try {
                javaClass.classLoader.getResourceAsStream(resourcePath).use { input ->
                    if (input != null) {
                        val targetFile = targetDir.resolve(libName)
                        Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING)
                        LOG.info("TFS: Extracted: $libName to $targetFile")

                        // 显式加载库
                        try {
                            System.load(targetFile.toString())
                            LOG.info("TFS: Loaded native library: $libName")
                        } catch (e: UnsatisfiedLinkError) {
                            LOG.error("TFS: Failed to load native library: $libName", e)
                            e.printStackTrace()
                        }
                    } else {
                        LOG.warn("TFS: Native library not found: $resourcePath")
                    }
                }
            } catch (e: IOException) {
                LOG.error("TFS: Failed to extract $libName", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取当前平台的本地库文件名列表
     */
    private fun getLibraryNamesForPlatform(platformPath: String): Array<String> {
        return when {
            platformPath.contains("win32") -> arrayOf(
                "native_misc.dll",
                "native_auth.dll",
                "native_console.dll",
                "native_credential.dll",
                "native_filesystem.dll",
                "native_messagewindow.dll",
                "native_registry.dll",
                "native_synchronization.dll"
            )
            platformPath.contains("macosx") -> arrayOf(
                "libnative_misc.jnilib",
                "libnative_auth.jnilib",
                "libnative_console.jnilib",
                "libnative_filesystem.jnilib",
                "libnative_keychain.jnilib",
                "libnative_synchronization.jnilib"
            )
            else -> arrayOf(
                "libnative_misc.so",
                "libnative_auth.so",
                "libnative_console.so",
                "libnative_filesystem.so",
                "libnative_synchronization.so"
            )
        }
    }

    /**
     * 递归删除目录
     */
    private fun deleteDirectory(path: Path) {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach { p ->
                    try {
                        Files.delete(p)
                    } catch (e: IOException) {
                        // 忽略
                    }
                }
        }
    }

    /**
     * Windows 上将 DLL 目录添加到 PATH 环境变量
     */
    private fun addToWindowsPath(dir: Path) {
        try {
            // 设置 java.library.path
            val existingLibPath = System.getProperty("java.library.path", "")
            val newLibPath = if (existingLibPath.isNotEmpty()) {
                "$dir;$existingLibPath"
            } else {
                dir.toString()
            }
            System.setProperty("java.library.path", newLibPath)
            LOG.info("TFS: Set java.library.path to include: $dir")

            // 尝试通过反射修改进程环境变量（Windows 需要）
            try {
                val processEnvClass = Class.forName("java.lang.ProcessEnvironment")
                val field = processEnvClass.getDeclaredField("theEnvironment")
                field.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val env = field.get(null) as MutableMap<String, String>
                val currentPath = env["PATH"] ?: env["Path"] ?: ""
                env["PATH"] = "${dir};$currentPath"
                LOG.info("TFS: Added to PATH: $dir")
            } catch (e: Exception) {
                LOG.warn("TFS: Could not modify PATH environment variable: ${e.message}")
            }
        } catch (e: Exception) {
            LOG.error("TFS: Failed to add to Windows PATH", e)
        }
    }
}
