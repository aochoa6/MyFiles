package com.example.myfiles;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout1);
    }

    private  boolean isFileManagerInitialized = false;
    private boolean[] selection;
    private File[] files;
    private List<String>filesList;
    private int filesFoundCount;
    private Button refreshButton;
    private File dir;
    private String currentPath;
    private boolean isLongClick;
    private int selectedItemIndex;
    private String copyPath;


    @Override
    protected void onResume(){
        super.onResume();

        //Request Permission for the user
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()){
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
        //Check if the File Manager is initialize or not
        if(!isFileManagerInitialized){

            /*Setting the current path to Downloads. Basically when you open
            the app , the contents of the Download directory will appear
            first*/
            currentPath = String.valueOf(Environment.
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));

            //Root path is setting on the /root
            final String rootPath = currentPath.substring(0,currentPath.lastIndexOf('/'));

            //PathOutput will show the path on where the user is currently in.
            final TextView pathOutput = findViewById(R.id.pathOutput);



            final ListView listView = findViewById(R.id.listView);
            final TextAdapter textAdapter1 = new TextAdapter();
            listView.setAdapter(textAdapter1);
            //arraylist that enables to set the data of the new files
            filesList = new ArrayList<>();

            refreshButton = findViewById(R.id.refresh);
            refreshButton.setOnClickListener(v -> {
                //set pathOutput to currentPath
                pathOutput.setText(currentPath.substring(currentPath.lastIndexOf('/')+1));

                //directory is a file, defined by the current path
                dir = new File(currentPath);

                //files = File array. current files are stored
                files = dir.listFiles();
                filesFoundCount = files.length;
                selection = new boolean[filesFoundCount];
                textAdapter1.setSelection(selection);

                filesList.clear(); //clear the arraylist
                for(int i = 0; i < filesFoundCount; i++){
                    filesList.add(String.valueOf(files[i].getAbsolutePath()));
                }
                textAdapter1.setData(filesList);
            });

            refreshButton.callOnClick();

            final Button goBackButton = findViewById(R.id.goBack);
            goBackButton.setOnClickListener(v -> {
                if(currentPath.equals(rootPath)){
                    return;
                }
                currentPath = currentPath.substring(0,currentPath.lastIndexOf('/'));
                refreshButton.callOnClick();
            });


            listView.setOnItemClickListener((parent, view, position, id) ->
                    new Handler().postDelayed(() -> {
                        //Long click = hold on the item
                        if(!isLongClick){
                            //Check if the select item is a folder or file
                            if (files[position].isDirectory()){
                                currentPath = files[position].getAbsolutePath();
                                refreshButton.callOnClick();
                            }
                        }
                    },50));

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    isLongClick = true;
                    selection[position] = !selection[position];
                    textAdapter1.setSelection(selection);
                    int selectionCount = 0;
                    for(boolean aSelection : selection){
                        if(aSelection){
                            selectionCount++;
                        }
                    }
                    if(selectionCount > 0){
                        if (selectionCount == 1) {
                            selectedItemIndex = position;
                            findViewById(R.id.rename).setVisibility(View.VISIBLE);
                        }else{
                            findViewById(R.id.rename).setVisibility(View.GONE);
                        }
                        findViewById(R.id.bottomBar).setVisibility(View.VISIBLE);
                    }else{
                        findViewById(R.id.bottomBar).setVisibility(View.GONE);
                    }
                    new Handler().postDelayed(() -> isLongClick = false,1000);
                    return false;
                }
            });

            final Button b1 = findViewById(R.id.b1); //Delete


            b1.setOnClickListener(v -> {
                final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                deleteDialog.setTitle("Delete");
                deleteDialog.setMessage("Do you want to delete this?");
                deleteDialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for(int i = 0; i < files.length; i++){
                            if(selection[i]){
                                deleteFileorFolder(files[i]);
                            }
                        }
                        refreshButton.callOnClick();
                    }
                });
                deleteDialog.setNegativeButton("No", (dialog, which) -> dialog.cancel());
                deleteDialog.show();
            });

            final Button createNewFolder = findViewById(R.id.newFolder);
            createNewFolder.setOnClickListener(v -> {
                final AlertDialog.Builder newFolderDialog =
                        new AlertDialog.Builder(MainActivity.this);
                newFolderDialog.setTitle("New Folder");
                final EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                newFolderDialog.setView(input);
                newFolderDialog.setPositiveButton("OK",
                        (dialog, which) -> {
                            final File newFolder = new File(currentPath+"/"+input.getText());
                            if(!newFolder.exists()){
                                newFolder.mkdir();
                                refreshButton.callOnClick();
                            }
                        });
                newFolderDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                newFolderDialog.show();
            });

            final Button renameButton = findViewById(R.id.rename);
            renameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder renameDialog =
                            new AlertDialog.Builder(MainActivity.this);
                    renameDialog.setTitle("Rename To: ");
                    final EditText input = new EditText(MainActivity.this);
                    final String renamePath = files[selectedItemIndex].getAbsolutePath();
                    input.setText(renamePath.substring(renamePath.lastIndexOf('/')));
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    renameDialog.setView(input);
                    renameDialog.setPositiveButton("Rename",
                            (dialog, which) -> {
                                String s = new File(renamePath).getParent() + "/"+input.getText();
                                File newFile = new File(s);
                                new File(renamePath).renameTo(newFile);
                                refreshButton.callOnClick();
                                selection = new boolean[files.length];
                                textAdapter1.setSelection(selection);
                            });
                    renameDialog.show();
                }
            });

            final Button copyButton = findViewById(R.id.copy);
            copyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyPath = files[selectedItemIndex].getAbsolutePath();
                    //Log.d("testing", copyPath);

                    selection = new boolean[files.length];
                    textAdapter1.setSelection(selection);
                    findViewById(R.id.paste).setVisibility(View.VISIBLE);
                }
            });

            final Button pasteButton = findViewById(R.id.paste);
            pasteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pasteButton.setVisibility(View.GONE);

                    //Destination path
                    String dstPath = currentPath + copyPath.substring(copyPath.lastIndexOf('/') );
                    copy(new File(copyPath), new File(dstPath));
                    files = new File(currentPath).listFiles();
                    selection = new boolean[files.length];
                    textAdapter1.setSelection(selection);

                    //refreshButton.callOnClick();
                }
            });

            isFileManagerInitialized = true;
        }else{
            refreshButton.callOnClick();
        }
    }

    private void copy(File source, File destination){
        try {
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(destination);
            byte[] buffer = new byte[1024];
            int length;
            while((length = in.read(buffer))>0){
                out.write(buffer, 0, length);
            }
            out.close();
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    class TextAdapter extends BaseAdapter{
        private List<String> data = new ArrayList<>();

        private boolean[] selection;

        public void setData(List<String> data){
            if(data != null){
                this.data.clear();
                if(data.size() > 0){
                    this.data.addAll(data);
                }
                notifyDataSetChanged();
            }
        }

        void setSelection(boolean[] selection){
            if(selection!=null){
                this.selection = new boolean[selection.length];
                for(int i = 0; i < selection.length; i++){
                    this.selection[i]=selection[i];
                }
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null){
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.textItem)));
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            final String item = getItem(position);
            holder.info.setText(item.substring(item.lastIndexOf('/')+1));
            if(selection!=null){

                if(selection[position]){
                    holder.info.setBackgroundColor(Color.argb(100,9,9,9));
                }else{
                    holder.info.setBackgroundColor(Color.WHITE);
                }

            }
            return convertView;
        }

        class ViewHolder{
            TextView info;

            ViewHolder(TextView info){
                this.info = info;
            }
        }

    }
    private static final int REQUEST_PERMISSIONS = 1234;
    private static final String[] PERMISSIONS ={
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int PERMISSIONS_COUNT = 2;
    private boolean arePermissionsDenied(){
        int p = 0;
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            while(p < PERMISSIONS_COUNT){
                if(checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED){
                    return true;
                }
                p++;
            }
        }
        return false;
    }

    private void deleteFileorFolder(File fileorFolder){
        if(fileorFolder.isDirectory()){
            if(fileorFolder.list().length == 0){
                fileorFolder.delete();
            }else{
                String files[] = fileorFolder.list();
                for(String temp : files){
                    File fileToDelete = new File(fileorFolder, temp);
                    deleteFileorFolder(fileToDelete);
                }
                if(fileorFolder.list().length == 0){
                    fileorFolder.delete();
                }
            }
        }else{
            fileorFolder.delete();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final String[] permissions, final int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if( requestCode == REQUEST_PERMISSIONS && grantResults.length >0){
            if(arePermissionsDenied()){
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }else{
                onResume();
            }
        }
    }




}