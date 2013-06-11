The JSustem SVN plugin allows you perform source control operations directly from the JSystem runner.
You can commit, update and revert scenarios and SUT file.

To connect the plugin to JSystem please add the following properties to your jsystem.properties file:

scm.class=org.jsystemtest.plugin.svn.SvnHandler
scm.repoistory=<repository>
scm.user=<user>
scm.password=<password>

You can also add them from the JSystem properties dialog.

After JSystem restart, check View->Toolbars->Source Control Toolbars and all the SVN operations will be presented.  




