#!/bin/bash

DC_USER=dcuser
DC_HOME=/home/dcuser
NB_ENV=nb_env

# Clone HTRC-JupyterNotebooks git repository into $DC_HOME
git clone https://github.com/htrc/HTRC-JupyterNotebooks.git $DC_HOME/HTRC-JupyterNotebooks
chown -R $DC_USER:$DC_USER $DC_HOME/HTRC-JupyterNotebooks
chmod -R 775 $DC_HOME/HTRC-JupyterNotebooks

# Set nb_env Python environment ($NB_ENV.tar.gz exists at https://analytics.hathitrust.org/files/nb_env.tar.gz)
cp ./$NB_ENV.tar.gz /opt/anaconda/envs
cd /opt/anaconda/envs
mkdir -p ./$NB_ENV
tar -xzf $NB_ENV.tar.gz -C $NB_ENV
cd -

# Add bash script and Desktop icon
cp -r JupyterNotebooks /opt/applications
chown -R $DC_USER:$DC_USER /opt/applications/JupyterNotebooks
cp /opt/applications/JupyterNotebooks/notebook.desktop $DC_HOME/Desktop
chown -R $DC_USER:$DC_USER $DC_HOME/Desktop/notebook.desktop

