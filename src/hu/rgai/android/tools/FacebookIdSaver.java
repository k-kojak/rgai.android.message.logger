package hu.rgai.android.tools;



import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQuery;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;
import hu.rgai.android.beens.fbintegrate.FacebookIntegrateItem;
import hu.rgai.android.config.Settings;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class saves Facebook id's to contact list, or updates if exists.
 * Saves the Facebook id's as NORMAL Im types, like Skype, but of course this is a Custom type,
 * since Android does not have built in Facebook type.
 * 
 * @author Tamas Kojedzinszky
 */
public class FacebookIdSaver {

  public FacebookIdSaver() {
    
  }
  
  public void integrate(Context context, FacebookIntegrateItem fbii) {
    ContactUpdateItem[] rawContactIdsToUpdate = this.getUserIdToUpdate(context, fbii.getName());
//    Log.d("rgai", "UPDATE COUNT -> " + rawContactIdsToUpdate.length);
    
//    Log.d("rgai", fbii.toString());
    String facebookName = fbii.getFbAliasId() != null && fbii.getFbAliasId().length() > 0 ? fbii.getFbAliasId() : fbii.getFbId();
    
    // Updating facebook ids
    for (ContactUpdateItem contact : rawContactIdsToUpdate) {
    
      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
      String where = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
      String[] params = new String[]{contact.id + "", ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE};
      ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
              .withSelection(where, params)
              .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
              .withValue(ContactsContract.Data.DATA1, facebookName)
              .withValue(ContactsContract.Data.DATA2, ContactsContract.CommonDataKinds.Im.TYPE_OTHER)
              .withValue(ContactsContract.Data.DATA5, ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM)
              .withValue(ContactsContract.Data.DATA6, Settings.Contacts.DataKinds.Facebook.CUSTOM_NAME)
              .withValue(ContactsContract.Data.DATA10, fbii.getFbId())
              .build());
      try {
        context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
      } catch (RemoteException ex) {
        Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
      } catch (OperationApplicationException ex) {
        Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    
    // If there was no update, then insert new facebook id to existing user
    if (rawContactIdsToUpdate.length == 0) {
//      Log.d("rgai", "THERE WAS NO UPDATE");
      ContactUpdateItem[] rawContactIdsToInsert = this.getUserIdToInsert(context, fbii.getName());
//      Log.d("rgai", "INSERT COUNT -> " + rawContactIdsToInsert.length);
    
      for (ContactUpdateItem contact : rawContactIdsToInsert) {

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
    //    int insertPos = ops.size();
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.id)

                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.Data.DATA1, facebookName)
                .withValue(ContactsContract.Data.DATA2, ContactsContract.CommonDataKinds.Im.TYPE_OTHER)
                .withValue(ContactsContract.Data.DATA5, ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM)
                .withValue(ContactsContract.Data.DATA6, Settings.Contacts.DataKinds.Facebook.CUSTOM_NAME)
                .withValue(ContactsContract.Data.DATA10, fbii.getFbId())
                .build());
        try {
          context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException ex) {
          Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
        } catch (OperationApplicationException ex) {
          Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      // The user (even the name) does not exists in the contact list, so lets create it
      if (rawContactIdsToInsert.length == 0) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
          
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
        .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, fbii.getName())
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.Data.DATA1, facebookName)
                .withValue(ContactsContract.Data.DATA2, ContactsContract.CommonDataKinds.Im.TYPE_OTHER)
                .withValue(ContactsContract.Data.DATA5, ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM)
                .withValue(ContactsContract.Data.DATA6, Settings.Contacts.DataKinds.Facebook.CUSTOM_NAME)
                .withValue(ContactsContract.Data.DATA10, fbii.getFbId())
                .build());
        // if inserting and has image, insert image to contact
        if (fbii.getThumbImgUlr() != null) {
          // insert thumbnail img
          InputStream is = null;
          try {
            is = (InputStream) new URL(fbii.getThumbImgUlr()).getContent();
          } catch (MalformedURLException ex) {
            Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
          } catch (IOException ex) {
            Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
          }
          Bitmap img = BitmapFactory.decodeStream(is);
          if (img != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            img.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, stream.toByteArray())
                    .build());
          }
          // insert full img
//          if (fbii.getFullImg() != null) {
            try {
//              stream = new ByteArrayOutputStream();
//              fbii.getFullImg().compress(Bitmap.CompressFormat.JPEG, 100, stream);
//              byte[] fullImgArray = stream.toByteArray();
              context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
//              long rawContactId = ContentUris.parseId(results[0].uri);
//              final Uri displayPhotoUri = Uri.withAppendedPath(
//                      ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId),
//                      ContactsContract.RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
//              final FileOutputStream photoStream = context.getContentResolver().openAssetFileDescriptor(displayPhotoUri, "rw").createOutputStream();
//              try {
//                int bufferSize = 16 * 1024;
//                for (int offset = 0; offset < fullImgArray.length; offset += bufferSize) {
//                  bufferSize = Math.min(bufferSize, (fullImgArray.length - offset));
//                  photoStream.write(fullImgArray, offset, bufferSize);
//                }
//              } finally {
//                photoStream.close();
//              }
            } catch (RemoteException ex) {
              Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
            } catch (OperationApplicationException ex) {
              Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (FileNotFoundException ex) {
//              Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (IOException ex) {
//              Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
            }
//          }
        }
      }
    }
    
  }
  
  private ContactUpdateItem[] getUserIdToUpdate(Context context, String name) {
    return getUserIds(context, name, true);
  }
  
  private ContactUpdateItem[] getUserIdToInsert(Context context, String name) {
    return getUserIds(context, name, false);
  }
  
  private ContactUpdateItem[] getUserIds(Context context, String name, boolean update) {
    name = replaceAccents(name);
    ContentResolver cr = context.getContentResolver();
    name = name.toUpperCase();
    String[] projection = new String[] {
        ContactsContract.Data.RAW_CONTACT_ID,
        ContactsContract.Data.DISPLAY_NAME_PRIMARY,
        ContactsContract.Data.MIMETYPE
    };
    
    String selection = "";
    String[] selectionArgs = null;
    if (update) {
      selection = "UPPER(" + ContactsContract.Data.DISPLAY_NAME_PRIMARY + ") LIKE ? "
            + " AND " + ContactsContract.Data.MIMETYPE + " = ? "
            + " AND " + ContactsContract.Data.DATA2 + " = ? "
            + " AND " + ContactsContract.Data.DATA5 + " = ?"
            + " AND " + ContactsContract.Data.DATA6 + " = ?";
      selectionArgs = new String[]{
              "%" + name + "%",
              ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE,
              ContactsContract.CommonDataKinds.Im.TYPE_OTHER + "",
              ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM + "",
              Settings.Contacts.DataKinds.Facebook.CUSTOM_NAME + ""
      };
    } else {
      selection = "UPPER(" + ContactsContract.Data.DISPLAY_NAME_PRIMARY + ") LIKE ? "
            + " AND " + ContactsContract.Data.MIMETYPE + " = ?";
      selectionArgs = new String[]{
              "%" + name + "%",
              ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
      };
    }
    
    Cursor cu = cr.query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, null);
    
    cu.moveToFirst();
    String rid = "-1";
    ArrayList<ContactUpdateItem> ids = new ArrayList<ContactUpdateItem>();
    while (!cu.isAfterLast()) {
      int ridIdx = cu.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID);
      rid = cu.getString(ridIdx);
      ids.add(new ContactUpdateItem(Long.parseLong(rid)));
      
      cu.moveToNext();
    }
    if (cu != null) {
      cu.close();
    }
    
    return ids.toArray(new ContactUpdateItem[ids.size()]);
    
  }
  
  private class ContactUpdateItem {
    long id;

    public ContactUpdateItem(long id) {
      this.id = id;
    }
  }
  
  private String replaceAccents(String val) {
    char[] replaces = new char[]{
      'ö','ü','ó','ő','ú','é','á','ű','í',
      'Ö','Ü','Ó','Ő','Ú','É','Á','Ű','Í'};
    for (char c : replaces) {
      val = val.replaceAll(c+"", "_");
    }
    
    return val;
  }
  
}
