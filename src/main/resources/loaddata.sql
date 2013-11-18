USE vmdb;

INSERT INTO images (imagename, imagepath, imagedescription) VALUES ("ubuntu-image", "/u/hathitrust/htrc-sloan/script-simulator/vm-image/ubuntu-image", "This is an Ubuntu image.");
INSERT INTO images (imagename, imagepath, imagedescription) VALUES ("suse-image", "/u/hathitrust/htrc-sloan/script-simulator/vm-image/suse-image", "This is a Suse image.");
INSERT INTO images (imagename, imagepath, imagedescription) VALUES ("redhat-image", "/u/hathitrust/htrc-sloan/script-simulator/vm-image/redhat-image", "This is a Redhat image.");
INSERT INTO policies (policyname, policypath) VALUES ("policy-1", "/u/hathitrust/htrc-sloan/script-simulator/firewall-policy/policy-1");
INSERT INTO policies (policyname, policypath) VALUES ("policy-2", "/u/hathitrust/htrc-sloan/script-simulator/firewall-policy/policy-2");