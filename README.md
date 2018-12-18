# Aion GUI

Aion Graphical UI module (modGui) is a front-end for the 
[Aion kernel](https://github.com/aionnetwork/aion).  It facilitates kernel 
management and provides basic wallet functionality.

## Development

It is distributed separately from the kernel and its source code maintained 
in a [separate repository](https://github.com/aionnetwork/aion_gui).  To include
it into in your Aion development environment:

1. If you have not already cloned the Git repository for the Aion kernel, do this first.  Instructions are available on its repository's Wiki: 
[https://github.com/aionnetwork/aion/wiki/Build-your-Aion-network](https://github.com/aionnetwork/aion/wiki/Build-your-Aion-network).
1. Ensure that the Git submodule is also cloned and up-to-date.  From the root
`aion` directory (where you cloned the Aion kernel), run the following:

        
        cd modGui
        git pull
        
1. Edit the Aion kernel's build script to include modGui by modifying the 
`gradle.properties` file in the `aion` root directory.  Any text editor will
work for this purpose.  You will find the following lines in the file:


        # Uncomment to include modGui in build
        # modGuiPath=modGui

1. Uncomment the line `modGuiPath=modGui` by removing the leading `#` character.
1. Now, invoke the usual kernel build task via Gradle and the GUI will also be 
built.

        ./gradlew build

1.  Run the GUI using `./aion_gui.sh`
