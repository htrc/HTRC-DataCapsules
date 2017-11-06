# This script installs negotiator-guest on the VM
# Refer to https://github.com/htrc/HTRC-DataCapsules/wiki/Setup-Data-Capsules-Host-and-Guests on the setup details for negotiator host

pip install negotiator-guest
mkdir -p /usr/lib/negotiator/commands
cp $USER_HOME_DIR/uploads/negotiator-guest.service /etc/systemd/system/negotiator-guest.service
systemctl enable negotiator-guest.service
cp $USER_HOME_DIR/uploads/fix-securevol-permissions /usr/lib/negotiator/commands/fix-securevol-permissions
chown $SSH_USERNAME /usr/lib/negotiator/commands/fix-securevol-permissions
chmod 744 /usr/lib/negotiator/commands/fix-securevol-permissions

cp $USER_HOME_DIR/uploads/99-udisks2.rules /etc/udev/rules.d/99-udisks2.rules
chown $SSH_USERNAME /etc/udev/rules.d/99-udisks2.rules
chmod 744 /etc/udev/rules.d/99-udisks2.rules
