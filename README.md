HTRC Data Capsule
================================

Overview
--------
Digital texts with access and use protections form a unique and fast growing collection of materials. Growing equally as quickly is the development of text and data mining algorithms that process large text-based collections for purposes of exploring the content computationally. There is a strong need for research to establish the foundations for secure computational and data technologies that can ensure a non-consumptive environment for use-protected texts such as the copyrighted works in the HathiTrust Digital Library. Non-consumptive research can be defined as as “research in which computational analysis is performed on one or more books, but not research in which a researcher reads or displays.” (in Google Book Settlement). Operationally, non-consumptive research requires that no action or set of actions on the part of users, either acting alone or in cooperation with other users over the duration of one or multiple sessions can result in sufficient information gathered from a collection of copyrighted works to reassemble pages from the collection. Non-consumptive research is, in and of itself, a research challenge which requires deeper study.

Developing a secure computation and data environment for non-consumptive research for the HathiTrust Research Center is funded through a grant from the Alfred P. Sloan Foundation. In this research, researchers at HTRC and the University of Michigan are developing a “data capsule framework” that is founded on a principle of “trust but verify”. That is, the informatics scholar is given freedom to experiment with new algorithms on a huge body of copyrighted or otherwise protected information, but technological mechanisms are in place to verify compliance with the policy of non-consumptive research. 
The project has resulted in a novel experimental framework that permits analytical investigation of a corpus but prohibits data from leaving the capsule.  The HTRC Data Capsule is both a system architecture and set of policies that enable computational investigation over the protected content of the HT digital repository that is carried out and controlled directly by a researcher. It leverages the foundational security principles of the Data Capsules of A. Prakash of University of Michigan, which allows privileged access to sensitive data while also restricting the channels through which that data can be released. 

HTRC Data Capsule works by giving a researcher their own virtual machine (VM) that runs within the HTRC domain.  The researcher can configure the VM as they would their own desktop with their own tools.  After they are done, the VM switches into a “secure mode”, where network and other data channels are restricted in exchange for access to the data being protected. Results are emailed to the user.

Installing
----------
You need to setup both the backend part and the web service part. Please refer to the [README] of backend and the [set up guide] of web service for general setup. 

[README]:https://github.com/htrc/HTRC-Data-Capsules/blob/master/backend/README
[set up guide]:https://github.com/htrc/HTRC-Data-Capsules/raw/master/webservice/Readme.pdf




