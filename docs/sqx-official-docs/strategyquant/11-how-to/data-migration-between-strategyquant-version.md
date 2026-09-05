# Data migration between StrategyQuant versions

- Source: <https://strategyquant.com/doc/strategyquant/data-migration-between-strategyquant-version/>
- Section: StrategyQuant X › How to...
- Fetched: 2026-09-04

---

## **Before you start migration process**

#### Never install a new StrategyQuant X to the same location, as you have previous installation. Always use the new destination. This prevents the issues.

We strongly recommend doing the backup of the user folder in SQ X install folder to prevent the data loss. Close the StrategyQuant app before doing a backup.

Do this migration process only one version up, for example from build 141 to build 142 otherwise you can get some issues with incompatibility. 

## **What to migrate**

Most user data is stored in the SQ X install folder/user/ directory.  So, if you only want to transfer data (e.g., **custom blocks**, **projects**, or **data etc.**), focus on these folders:

## **List of folder where are stored the data:**

- Custom Blocks – for **custom blocks are stored in this file settings/blockGroups.xml**
- Custom Groups – for **custom groups are stored in this file settings/customBlocks.xml**
- Data folder – (and possibly custom\_data/) **for historical** **data feed**
- Extend folder – with custom indicators and snippets. Sometimes this can cause issues because of java change in the SQX and in this case, the plugins or snippets have to be compiled again in the code editor.
- projects folder – here are stored all strategies, data banks, and custom projects.
- SQ4Business – projects/ for SQ4Business **projects**
- Settings/ folder for all **settings** (including default conditions and configuration files, data bank views)
- Backups for custom blocks and groups are stored in settings / block groups – backups and custom blocks – backups
- Custom Data – in this folder are stored data for external indicators
- Templates – in this folder are stored templates, if you saved here some templates, you need to also move this folder
- Data bank views – are stored in this folder SQX install folder\user\settings\views\

Strategies – if you have some strategies in this folder SQX install folder\user\strategies\ you need to move them also, but StrategyQuant does not save here strategies as default.

## Video tutorial:

Transcript from the video:

In this video, we will show and explain how to migrate data between StrategyQuant versions.  
Before we even get into migrating data between versions, it should be mentioned  
that if you are going to upgrade, we do not currently recommend installing or overwriting  
existing versions of StrategyQuant. This is because the Java version may change between  
StrategyQuant versions and if you would possibly use any custom indicators or snippets,  
they may not be compatible between versions. Thus, if you install a new version, always install it  
in a very new directory. Never install a new version of StrategyQuant into an existing one.  
Before we start migrating new data into StrategyQuant, even though you may have the original  
installation, it is recommended that you make a copy in zip format so you have a backup that  
is separate from it. And you should, of course, keep the previous installation of StrategyQuant  
closed so that the data is saved and backed up properly. Another note on upgrading is that we  
recommend only ever upgrading up one version. That is, if you have version 1.41, for example,  
you should migrate to version 1.42 at most. This is because we change the structure within versions  
and always focus on upgrading between versions. So, if you were to migrate from, say, version 1.38  
to 1.42, it most likely will not work for you. Now, let’s take a look at where StrategyQuant  
has its data stored. All data is stored in the StrategyQuant installation directory  
in the User folder. So, if you are going to backup any data, you need to backup your User folder,  
which you can see here. Just backup this folder. You can send it to a zip like this,  
or if you are using some software like 7-Zip, you can choose Add to Archive and it will create  
a backup. So, simply, this is where all the backups are stored. Otherwise, a tip.  
I personally prefer Commander with two windows, which makes moving and copying files a lot easier  
because using keyboard shortcuts makes my work even more efficient. So, such a tip for you  
might be DoubleCommander. It’s a fully open source and free solution. You can download it at  
wcmd.sourceforge.io. Just click on Download and you can choose between the portable version  
or the installation directory. So, I’m going to explain this whole process of data migration  
using this program because it adds a lot of efficiency to this very process.  
Let’s go back and let’s tell you where the data is stored and let’s illustrate it.  
Custom groups and blocks are located in the StrategyQuantUser folder,  
concretely in the Settings folder. And we can see that I have some custom groups here,  
but I do not have any custom blocks because those are only saved in this file if you have added any.  
Then, here we have the Data folder itself.  
We’ll find it here and this folder is where all the historical data is stored.  
Then, there’s the Extent folder. This is where all the plugins, snippets, external indicators,  
and so on are stored. The next one is Projects directory and this is where all the projects  
are stored. You also have all the strategies from the data bank stored here. If we open this up,  
we can see the directory structure of a particular Retester project  
or custom projects and we can see the strategy stored here, for example.  
If StrategyQuant ever crashed unexpectedly, we have an automatic data synchronization feature  
and all your data would be synchronized to this directory. So, even if unexpectedly your computer  
would shut down, for example, the synchronization takes place every hour and alternatively,  
you can shorten this period. However, StrategyQuant would either load the data automatically the  
next time you start it or if you don’t run it, you would track down the strategies in these  
directories. That’s an important thing to know. Then, there’s the SQ for Business feature. I don’t  
think I have a prototype here right now, but you would see StrategyQuant for Business in that user  
directory and then you would copy that entire directory into a new installation of Strategic  
One. Another one we have here is Settings directory, which is where you would just store  
the backups from custom blog groups or custom blogs or other things. In short,  
what I’ve already mentioned is found in the Settings directory.  
Next, Custom Data. Here, we would find all the data for the external custom minicares.  
And last, here we have Templates. This is where the default templates are stored and you can,  
of course, store your own templates here too, but in that case, if you were to store any  
templates here, you would have to copy those templates as well. Moving on, we also have  
Data Bank Use, which is the last item on this list and this use can be found right here.  
So, if we had some custom views for data banks, we would copy this entire directory right here  
into the new StrategyQuant. And one more piece of practical advice. This double commander is  
great to use with a keyboard and it eliminates a lot of clicking, which I personally don’t like  
and I avoid mouse clicking as much as possible. A few more practical hints just about the double  
commander and the controls in general. We can move around with the arrow keys. If you hit  
Enter, you go to a directory below or if you are here on this arrow, you go one directory above.  
If we want to copy the data, press the F5 key and a dialog box pops up like this and we just  
copy the data. If we want to cancel the action, we can press the Escape key and this dialog box  
will close. If we just want to move the data, we can press F6 and the data will be moved.  
To create a new directory, press the F7 key and then create a directory dialog pop-up.  
However, I do believe that even mouse navigation is very intuitive and it will definitely make  
you more efficient when migrating between versions. So, that’s it for the quick tip and  
let’s get back to migrating the data. Now, let’s take a quick look at how to migrate the data.  
I’ve created a new installation of Strategic 142 video here and here I’m going to delete  
this directory, for example, and I’m going to move it all incrementally. This is a new  
installation and I do not need to back up anything here, but in the original installation,  
I recommend, just as I mentioned at the beginning of this video, make a backup of the entire user  
directory. Now, we are going to copy the data one by one. So, I’m going to press the F5 key  
and a dialog box will pop up where I want to copy the data and I can either click Add Queue or I can  
just click on Start. Double Commander will start copying my data. The data has been copied now  
and we can copy the Extent folder, then copy the custom blocks to Settings, custom block groups,  
and also Algorithm.Cloud. Let’s override that. And then from this installation, we have custom  
projects here, which we could simply copy over like this. That’s one option. Or we could save  
the custom project somewhere on disk in our computer under a specific name and then easily  
open this custom project in a new StrategyQuant version. I’m going to show here, for example,  
how to copy a custom project for Bollinger Bands. I click on Enter and immediately the  
custom project is copied just like that. There is no need to migrate the logs because they are  
basically useless. And from the basic package that I have used in this StrategyQuant, that’s it.  
Anyway, if you use more than that or you have imported some views directly for databanks,  
you would copy them just this way and then run StrategyQuant. And that’s it for now. If you  
would have any questions, please post them in the comments and I look forward to seeing you  
in the next video.
