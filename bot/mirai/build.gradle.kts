plugins {
    id("java")
    id("net.mamoe.mirai-console") version ("2aa96098bb") apply true
}

dependencies {
    implementation(project(":core"))
    implementation(project(":script"))
    implementation(project(":service"))
    implementation(project(":bot:shared"))
    implementation("net.mamoe.yamlkt:yamlkt:0.13.0")
    compileOnly("org.openjdk.nashorn:nashorn-core:15.4")
    testConsoleRuntime("top.mrxiaom.mirai:overflow-core:1.0.4.600-ead8d02-SNAPSHOT")
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
    noTestCore = true
    setupConsoleTestRuntime {
        // 移除 mirai-core 依赖
        classpath = classpath.filter {
            !it.nameWithoutExtension.startsWith("mirai-core-jvm")
        }
    }
}