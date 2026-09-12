{
  description = "TimeTracker Android dev environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

        androidComposition = pkgs.androidenv.composeAndroidPackages {
          cmdLineToolsVersion = "13.0";
          toolsVersion = "26.1.1";
          platformToolsVersion = "37.0.1";
          buildToolsVersions = [ "35.0.0" ];
          platformVersions = [ "35" ];
          includeEmulator = true;
          emulatorVersion = "37.2.4";
          includeSystemImages = true;
          systemImageTypes = [ "google_apis" ];
          abiVersions = [ "x86_64" ];
          includeNDK = false;
          includeSources = false;
          useGoogleAPIs = true;
        };

        androidSdkRoot = "${androidComposition.androidsdk}/libexec/android-sdk";
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = [
            pkgs.jdk21
            androidComposition.androidsdk
          ];

          ANDROID_HOME = androidSdkRoot;
          ANDROID_SDK_ROOT = androidSdkRoot;
          GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdkRoot}/build-tools/35.0.0/aapt2";

          shellHook = ''
            export PATH="${androidSdkRoot}/platform-tools:${androidSdkRoot}/emulator:$PATH"
          '';
        };
      });
}
