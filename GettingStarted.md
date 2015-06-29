# Getting Started with IntelliJ IDEA and Android Development #

### Download IntelliJ IDEA ###

If you don't already have IntelliJ IDEA installed, you can download a 30-day evaluation version from http://www.jetbrains.com/idea/download/

The plugin requires IntelliJ IDEA 7.0 or higher and is not compatible with earlier versions of IntelliJ IDEA.

### Install Plugin ###

Go to Settings | Plugins, select the "Android Support" plugin in the Available tab and press Install.

### Create New Project ###

Select "Create project from scratch" option. On the second page of the wizard, enter the name and location of your project.

![http://idea-android.googlecode.com/files/NewProjectName.png](http://idea-android.googlecode.com/files/NewProjectName.png)

Accept the default source root option. On the "Technologies" page, select Android and enter the path to your Android SDK installation.

![http://idea-android.googlecode.com/files/androidSupport.png](http://idea-android.googlecode.com/files/androidSupport.png)

If you are prompted to select a JDK, select the installation home of a Java 1.5 or 1.6 JDK (no JRE).

### Create Activity ###

Select the src directory in the project view, press Alt-Ins (Ctrl-Enter on a Mac) to invoke the "New" menu and select "Package". Enter the name of the package.

Select the package, press Alt-Ins again and select "Activity" from the menu.

![http://idea-android.googlecode.com/files/androidNewActivity.png](http://idea-android.googlecode.com/files/androidNewActivity.png)

In the "New Activity" dialog, enter the class name and label for the activity. Ensure that "Mark as startup activity" checkbox is checked.

![http://idea-android.googlecode.com/files/newActivityDialogSmall.png](http://idea-android.googlecode.com/files/newActivityDialogSmall.png)

### Create Activity Implementation ###

In the activity class, press Ctrl-O to invoke the "Override Methods" dialog. Select the `onCreate` method.

Type the method body text:
```
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        TextView tv = new TextView(this);
        tv.setText("Hello, Android, from IntelliJ IDEA!");
        setContentView(tv);
    }
```

### Run the Activity ###

Right-click the class and select "Run HelloWorld" from the context menu.

![http://idea-android.googlecode.com/files/runActivity.png](http://idea-android.googlecode.com/files/runActivity.png)

Wait for the emulator to start, and for the activity to be displayed.

![http://idea-android.googlecode.com/files/androidEmulator.png](http://idea-android.googlecode.com/files/androidEmulator.png)

If an error "Cannot start activity..." is printed in the console window, close the emulator and try again (this is a known issue with the Android emulator).

Note that the initial release of the plugin does not support debugging Android applications. Debugging support will be added in a future release.

### Cool Stuff to Try ###

(a.k.a. why we're better than ADT :) )

In AndroidManifest.xml, put the cursor on @drawable/icon and press Ctrl-Shift-I.

![http://idea-android.googlecode.com/files/iconImpl.png](http://idea-android.googlecode.com/files/iconImpl.png)

Try pressing Ctrl-B and Ctrl-Space in a few places to explore the navigation completion options available. Even in the initial release of the plugin, a few things are supported.

![http://idea-android.googlecode.com/files/activityCompletion.png](http://idea-android.googlecode.com/files/activityCompletion.png)