Box Android Browse SDK
==============
This SDK enables the ability to easily browse through Box storage to pick a folder/file.

Quickstart
--------------
The SDK can be obtained by adding it as a maven dependency, cloning the source into your project, or by downloading one of the precompiled JARs from the releases page on GitHub.

This SDK has the following dependencies and will need to be included in your project:
* [minimal-json v0.9.1](https://github.com/ralfstx/minimal-json) (maven: `com.eclipsesource.minimal-json:minimal-json:0.9.1`)
* [box-content-sdk](https://github.com/box/box-android-content-sdk) (maven: `coming soon`)

Example:
--------------
```java
    // You will need a BoxSession and the BoxItem from the box-content-sdk 
    // Please refer to the documentation on the box-content-sdk for additional details.
    BoxSession session = new BoxSession(MainActivity.this);
    BoxFolder folder = new BoxApiFolder(session).getInfo("<FOLDER_ID>").send();
    
    // To launch the activity to browse a given folder and pick a file:
    startActivityForResult(BoxBrowseFileActivity.getLaunchIntent(MainActivity.this, "<FOLDER_ID>", session), "<YOUR_REQUEST_CODE>");

    // To receive the result from the file picker after you picked a file, implement onActivityResult in your activity:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode ==  "<YOUR_REQUEST_CODE>") {
            if (resultCode == Activity.RESULT_OK) {
                BoxFile boxFile = (BoxFile) data.getSerializableExtra(FilePickerActivity.EXTRA_BOX_FILE);
                // Your own code to handle boxFile goes here.
            } else {
                // Your error handling code.
            }
        }
    }

    // To launch the activity to browse a given folder and pick a folder:
    startActivityForResult(BoxBrowseFolderActivity.getLaunchIntent(MainActivity.this, "<FOLDER_ID>", session),  "<YOUR_REQUEST_CODE>");

    // To receive the result from the file picker after you picked a folder, implement onActivityResult in your activity:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode ==  "<YOUR_REQUEST_CODE>") {
            if (resultCode == Activity.RESULT_OK) {
                BoxFolder boxFolder = (BoxFolder) data.getSerializableExtra(FolderPickerActivity.EXTRA_BOX_FOLDER);
                // Your own code to handle boxFile goes here.
            } else {
                // Your error handling code.
            }
        }
    }
```