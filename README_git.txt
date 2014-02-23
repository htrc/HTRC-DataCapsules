
Basic git commands:


To add files, do a local commit, and to push to the server, respectively:

% git add .
% git commit -m "msg"
% git push origin master

(You can do 
% git push 
subsequently without origin and master arguments)

To remove a file:

% git rm filename

To pull changes and merge them with the current branch: 

% git pull

You should do git git pretty frequently.


Conflicts
---------

You may get a conflict when attempting to push. In that case, the following may work:


git pull        
edit the file with the conflict and resolve the conflict. Look for diff change marks.

Then recheck it in.

git add .
git commit -m "msg"
git push
git pull







