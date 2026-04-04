
# VSplit

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/vsplit?color=4&label=Downloads&logo=modrinth)](https://modrinth.com/mod/vsplit/versions)
[![CurseForge Downloads](https://cf.way2muchnoise.eu/vsplit.svg)](https://www.curseforge.com/minecraft/mc-mods/vsplit/files/all)

This mod provides the ability to split Valkyrien Skies ship when blocks disconnects.  
By default, only blocks that are direct neighbours can be connected. Some special blocks, such as torch and button, has special logic to only connect blocks it currently attached on.  

Modded blocks can also has themselves logic by implement vtil's [`IBlockAnchor`](https://github.com/LiterMC/vtil/blob/1.20.1/common/src/main/java/com/github/litermc/vtil/api/connectivity/IBlockAnchor.java) interface.
Ship attachments can implement [`ISplitListener`](https://github.com/LiterMC/VSplit/blob/1.20.1/common/src/main/java/com/github/litermc/vsplit/api/attachment/ISplitListener.java) to listen when a split happens and define special logic about it.
a