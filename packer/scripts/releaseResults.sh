# This script installs release results script

cp $USER_HOME_DIR/uploads/releaseresults /usr/local/bin/releaseresults
chown $SSH_USERNAME /usr/local/bin/releaseresults
chmod 744 /usr/local/bin/releaseresults
