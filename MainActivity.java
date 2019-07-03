package net.magnistudio.poppad.poppad;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {
    private static final String NEVERSAVED = "(untitled)";
    private static final String LOGTAG = "Log Tag";
    private static final int REQUEST_WRITE_EXTSTO = 20;
    private static final int REQUEST_READ_EXTSTO = 21;
    private static final int ALPHA_DISABLED = 130;
    private static final int ALPHA_ENABLED = 255;

    private boolean isSaved = true;
    private String filename = NEVERSAVED;

    private TextView statusBar;
    private EditText mainEdit;

    private String getResStr(int id)
    {
        return getString(id);
    }
    private String getResStrFmt(int id, Object...ap)
    {
        return getString(id, ap);
    }

    private AlertDialog.Builder prepErrDlg(String title, String msg)
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setIcon(R.drawable.ic_error_black_24dp);
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        return alertDialog;
    }

    private void update(boolean newSaveState)
    {
        if (isSaved = newSaveState) {
            statusBar.setText(getResStrFmt(R.string.sbTextSaved, filename));
        } else {
            statusBar.setText(getResStrFmt(R.string.sbTextUnsaved, filename));
        }
        invalidateOptionsMenu();
    }

    public File getDocsDir()
    {
        File file = new File(Environment.getExternalStorageDirectory() +
                                     File.separator + "Documents");
        if (!file.mkdirs())
            Log.e(LOGTAG, getResStr(R.string.err_dirNotCreated));
        return file;
    }
    public boolean fileExistsInDocs(String filePath)
    {
        File docs = getDocsDir();
        File file = new File(docs, filePath);
        return file.exists();
    }

    private void askAboutSaveBeforeOpeningAnother()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResStr(R.string.titleConfirmSave));
        builder.setMessage(getResStrFmt(R.string.promptSaveBeforeOpen, filename));
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                if (filename.equals(NEVERSAVED))
                    onSavingBrandNew(false);
                else {
                    try {
                        saveFile();
                    } catch (IOException e) {
                        AlertDialog.Builder dlg = prepErrDlg(
                                getResStr(R.string.err_titleErr),
                                e.getMessage());
                        dlg.setPositiveButton("OK",new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                dialogInterface.dismiss();
                            }
                        });

                    }
                }
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void askAboutSaveAndExit()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResStr(R.string.titleConfirmSave));
        builder.setMessage(getResStrFmt(R.string.promptSave, filename));
        builder.setPositiveButton(R.string.cmd_saveAndExit, new DialogInterface.
                OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                if (filename.equals(NEVERSAVED))
                    onSavingBrandNew(true);  /* true closes activity; must be
                                                done there because AlertDialog
                                                doesn't block */
                else {
                    try {
                        saveFile();
                    } catch (IOException e) {
                        AlertDialog.Builder dlg = prepErrDlg(
                                getResStr(R.string.err_titleErr),
                                e.getMessage());
                        dlg.setPositiveButton("OK",new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                dialogInterface.dismiss();
                            }
                        });

                    }
                    MainActivity.this.finish();
                }
            }
        });
        builder.setNegativeButton(getResStr(R.string.cmd_exitNoSave), new
                DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                MainActivity.this.finish();
            }
        });
        builder.show();
    }

    // SAVE FILE
    private static boolean validateFilename(String fn)
    {
        return Pattern.matches("[.]*\\w[\\w| .]*", fn);
    }

    private static String adjustFilename(String fn)
    {
        String ext = fn.substring(fn.lastIndexOf('.') + 1, fn.length());
        if (!ext.equals("txt"))
            fn += ".txt";
        return fn;
    }

    private void getPermsWrite()
    {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            try {
                saveFile();
            } catch (IOException e) {
                AlertDialog.Builder dlg = prepErrDlg(
                        getResStr(R.string.err_titleErr),
                        e.getMessage());
                dlg.setPositiveButton("OK",new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        dialogInterface.dismiss();
                    }
                });
            }
        } else
            ActivityCompat.requestPermissions(this,
                                              new String[] {
                                                      Manifest.permission.
                                                      WRITE_EXTERNAL_STORAGE },
                                                      REQUEST_WRITE_EXTSTO);
    }

    private void saveFile() throws IOException
    {
        File docsDir = getDocsDir();
        File file = new File(docsDir, filename);
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(mainEdit.getText().toString());
            fw.close();

            update(true);
        } catch (IOException e) {
            Log.e(LOGTAG, e.getMessage());
            throw e;
        }
    }

    /* common entry point for saving: When user saves file for first time and
       when user saves file again
     */
    private void preSaveFile(String dirtyFilename)
    {
        if (validateFilename(dirtyFilename)) {
            filename = adjustFilename(dirtyFilename);
            getPermsWrite();
        } else {
            Toast.makeText(this,
                       getResStrFmt(R.string.err_invalFilename, dirtyFilename),
                       Toast.LENGTH_LONG).show();
        }
    }

    // called when the file is being saved for the first time
    private void onSavingBrandNew(final boolean needToCloseAfter)
    {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(getResStr(R.string.titleSaveBrandNew));
        alertDialog.setMessage(getResStr(R.string.promptFilename));
        alertDialog.setView(input);

        alertDialog.setPositiveButton("Save",
                                      new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                String dirtyFilename = input.getText().toString();
                preSaveFile(dirtyFilename);
                if (needToCloseAfter)
                    MainActivity.this.finish();
                else
                    dialogInterface.dismiss();
            }
        });
        alertDialog.setNegativeButton("Cancel",
                                      new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                if (needToCloseAfter)
                    MainActivity.this.finish();
                else
                    dialogInterface.dismiss();
            }
        });

        alertDialog.show();
    }

    // OPEN FILE
    private void getPermsRead()
    {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            try {
                openFile();
            } catch (IOException e) {
                AlertDialog.Builder dlg =
                        prepErrDlg(getResStr(R.string.err_titleErr),
                                   e.getMessage());
                dlg.setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        dialogInterface.dismiss();
                    }
                });
            }
        } else
            ActivityCompat.requestPermissions(this,
                                              new String[] {
                                                      Manifest.permission.
                                                  READ_EXTERNAL_STORAGE },
                                              REQUEST_READ_EXTSTO);

    }

    private void onOpenFile()
    {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResStr(R.string.titleOpenFile));
        builder.setMessage(getResStr(R.string.promptFilenameOpen));
        builder.setView(input);
        builder.setPositiveButton("Open", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                preOpenFile(input.getText().toString());
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }
    private void preOpenFile(String dirtyFilename)
    {
        String filenameTemp;
        if (validateFilename(dirtyFilename)) {
            filenameTemp = adjustFilename(dirtyFilename);
            if (fileExistsInDocs(filenameTemp))
                filename = filenameTemp;
            else
                Toast.makeText(this, getResStrFmt(R.string.err_fileNoExist,
                                          filenameTemp), Toast.LENGTH_LONG)
                        .show();

            getPermsRead();
        } else {
            Toast.makeText(this, getResStrFmt(R.string.err_invalFilename,
                                dirtyFilename), Toast.LENGTH_SHORT).show();
        }
    }
    private void openFile() throws IOException
    {
        File docsDir = getDocsDir();
        File file = new File(docsDir, filename);
        String data;

        try {
            data = readFile(file);

            mainEdit.setText(data);
            // position cursor at the end
            mainEdit.setSelection(data.length());

            update(true);
        } catch (IOException e) {
            Log.e(LOGTAG, e.getMessage());
            throw e;
        }
    }

    @NonNull
    public static String readFile(File file) throws IOException
    {
        return readFile(file.getAbsolutePath(), Charset.defaultCharset());
    }

    @NonNull
    public static String readFile(String file, Charset cs) throws IOException
    {
        // No real need to close the BufferedReader/InputStreamReader
        // as they're only wrapping the stream
        FileInputStream stream = new FileInputStream(file);
        try {
            Reader reader =new BufferedReader(new InputStreamReader(stream,cs));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } finally {
            // Potential issue here: if this throws an IOException,
            // it will mask any others. Normally I'd use a utility
            // method which would log exceptions and swallow them
            stream.close();
        }
    }

    // ABOUT
    private void showAboutDlg()
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(getResStr(R.string.titleAbout));
        alertDialog.setMessage(getResStrFmt(R.string.msgAbout,
                                            BuildConfig.VERSION_NAME));
        alertDialog.setPositiveButton(
                "OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        dialogInterface.dismiss();
                    }
                });
        alertDialog.setIcon(R.mipmap.ic_launcher);
        alertDialog.show();
    }

    // EVENT HANDLERS
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainEdit = (EditText) findViewById(R.id.mainEdit);
        statusBar = (TextView) findViewById(R.id.statusbar);

        mainEdit.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence,
                                          int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start,
                                      int before, int n)
            {
                update(false);
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);

        MenuItem saveItem = menu.findItem(R.id.cmd_save);
        saveItem.setEnabled(!isSaved);
        saveItem.getIcon().setAlpha(isSaved ? ALPHA_DISABLED : ALPHA_ENABLED);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.cmd_save:
                if (filename.equals(NEVERSAVED))
                    onSavingBrandNew(false);
                else
                    preSaveFile(filename);
                return true;
            case R.id.cmd_saveas:
                onSavingBrandNew(false);
                return true;
            case R.id.cmd_open:
                if (!isSaved)
                    askAboutSaveBeforeOpeningAnother();
                onOpenFile();
                return true;
            case R.id.cmd_about:
                showAboutDlg();
                return true;
            case R.id.cmd_exit:
                this.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed()
    {
        if (!isSaved)
            askAboutSaveAndExit();
        else
            MainActivity.this.finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int grantResults[])
    {
        switch (requestCode) {
            case REQUEST_WRITE_EXTSTO:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        saveFile();
                    } catch (IOException e) {
                        AlertDialog.Builder dlg =
                                prepErrDlg(getResStr(R.string.err_titleErr),
                                           e.getMessage());
                        dlg.setPositiveButton("OK", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                dialogInterface.dismiss();
                            }
                        });
                    }
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.err_titleNeedPerms);
                    builder.setMessage(getResStr(R.string.err_needWritePerms));
                    builder.setPositiveButton("Exit", new DialogInterface.
                            OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int
                                i)
                        {
                            System.exit(1);
                            dialogInterface.dismiss();
                        }
                    });
                }
                return;
            case REQUEST_READ_EXTSTO:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        openFile();
                    } catch (IOException e) {
                        AlertDialog.Builder dlg =
                                prepErrDlg(getResStr(R.string.err_titleErr),
                                           e.getMessage());
                        dlg.setPositiveButton("OK", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                dialogInterface.dismiss();
                            }
                        });
                    }
                }
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getResStr(R.string.err_titleNeedPerms));
                    builder.setMessage(getResStr(R.string.err_needReadPerms));
                    builder.setPositiveButton("Exit", new DialogInterface.
                            OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int
                                i)
                        {
                            System.exit(1);
                            dialogInterface.dismiss();
                        }
                    });
                }
                return;
        }
    }
}