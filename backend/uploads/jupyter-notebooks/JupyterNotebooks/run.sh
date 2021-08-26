#!/bin/bash

GIT_REPO_PATH="/home/dcuser/HTRC-JupyterNotebooks"

cd $GIT_REPO_PATH
export BROWSER=google-chrome
source activate nb_env
jupyter notebook