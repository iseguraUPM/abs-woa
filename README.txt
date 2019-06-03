Group 1 WoA implementation
-------------------------------------
Juan Pablo Melgarejo
Iñaki Segura
Martin Zumarraga

The Group1_WoA-full.jar contains all the necessary libraries to run the program. The main agent that must be launched from JADE is AgPlatform.class . This agent
looks for other tribes in the es.upm.woa.groupX.agent classpath and launches them at the start of the program, where X is the group name.
The best way to launch this is including other groups' .jar files with the -cp option in java.

The program requires a configuration to be in the directory where it's launched from called "woa.properties"
the contents (in plain text) of the file are as follows (do not include comments between <>):

    map_path = <map .json configuration file path>
    reg_millis = 5000
    tick_millis = 300
    game_ticks = 10000
    gui_endpoint = 127.0.0.1:3000 <or other ip + port combination where the GUI is running>

    resource_cap = 800
    store_upgrade_amount = 200

    max_tribes = 5

