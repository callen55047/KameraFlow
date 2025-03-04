package environment

object ModuleArtifacts {
    object Core : IModuleArtifact {
        override val name: String = "Core"
        override val webNPMName: String = "core-web"
    }

    object App : IModuleArtifact {
        override val name: String = "App"
        override val webNPMName: String = "app-web"
    }
}

interface IModuleArtifact {
    val name: String

    val webNPMName: String
        get() = ""
}
