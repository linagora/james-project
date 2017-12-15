
During the past few weeks, the team has been working hard to make new features available in the OpenPaaS MVP release.


The team implemented JMAP sharing, allowing one to share a folder with another user of the domain. The sharee gets the right, if allowed, to read, mark delete and moves emails. This is especially useful to delegate the management of a specific mailbox to someone else. You can see a quick [video](https://www.youtube.com/watch?v=iKygmVKH-xU) presenting JMAP sharing feature in INBOX.


We also implemented drafts. The email you compose can now be saved in INBOX, reopened, re-edited. Moving them to the Outbox mailbox using the [JMAP protocol](https://jmap.io) will actually trigger the sending. You can see a quick [video](https://www.youtube.com/watch?v=iKygmVKH-xU) presenting the draft feature in INBOX.


After implementing these features, we were also involved in the "polish" sprint (see above?), allowing us to fix some little but annoying issues.


In the coming months, the team will be focused on delivering a highly reliable software. We will move toward latest states of the JMAP specifications and implement JMAP instant notifications (push), make James more distributed, and improve distributed safety.
