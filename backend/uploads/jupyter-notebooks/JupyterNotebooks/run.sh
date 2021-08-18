#!/bin/bash

VOLUME_LIST_PATH="/home/dcuser/HTRC/htrc-id"

# Get the volume ID list path
echo Plese enter the absolute path of the volume list\(Default path $VOLUME_LIST_PATH\).
read idlist_input

if [ -z "$idlist_input" ]
then
	echo Input for volume list path is empty. Get the default volume list path $VOLUME_LIST_PATH.
else
        VOLUME_LIST_PATH=$idlist_input
fi

echo Downloading volumes of $VOLUME_LIST_PATH
htrc download -f $VOLUME_LIST_PATH

cd /home/dcuser/HTRC-JupyterNotebooks
export BROWSER=google-chrome
source activate nb_env
jupyter notebook