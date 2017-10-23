
How to Run
==========

To run packer, use the command
'packer build ubuntu.json'

You are able to change the URLs of the installation packages in the ubuntu.json variables section
(located at the bottom of the ubuntu.json file).

Once you build the config file, the VM output would be available in the output-ubuntu1604 directory. 

Scripts
=======

The scripts are located in the ./scripts directory.
The following are the available scripts:

-desktop.sh 
-grubfix.sh 
-installAnaconda.sh 
-installJava.sh 
-installPackages.sh
-installR.sh 
-installScala.sh 
-installSpark.sh 
-negotiator-guest.sh 
-postprocessor.sh

Configuration
==========
You can make changes to either the configuration file (ubuntu.json) or the scripts.

If you make a change to the configuration file (ubuntu.json),
you can validate the config file using the command,

'packer validate ubuntu.json'


Setup Details
===================

This configuration file would install,

Oracle JDK 8 
R
Spark (/opt/spark)
Anaconda (/opt/anaconda)
Scala
SBT
Pytables

Following system level packages:
git
maven 
python-pip 
parallel 
curl 
htop 
iotop 
jq 
pcregrep 
zsh 
python3-pip

Following libraries would be installed for both python 2.7 and 3 :
numpy 
scipy 
matplotlib 
pandas 
nltk 
regex 
GenSim 
ujson 
dask 
toolz 
theano 
csvkit 
htrc-feature-reader



