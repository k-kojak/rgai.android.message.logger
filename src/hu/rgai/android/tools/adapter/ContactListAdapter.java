package hu.rgai.android.tools.adapter;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import hu.rgai.android.config.Settings;
import hu.rgai.android.intent.beens.FacebookRecipientAndr;
import hu.rgai.android.intent.beens.RecipientItem;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.view.AutoCompleteRow;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author konyak
 */
public class ContactListAdapter extends CursorAdapter implements Filterable {

  private ContentResolver mContent;
  private Set<String> allowedMimeTypes = null;
  private static final String DISPLAY_NAME = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Data.DISPLAY_NAME_PRIMARY : ContactsContract.Data.DISPLAY_NAME);

  public ContactListAdapter(Context context, Cursor c) {
    super(context, c);
    mContent = context.getContentResolver();
    allowedMimeTypes = new HashSet<String>();
    allowedMimeTypes.add(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
    allowedMimeTypes.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
    allowedMimeTypes.add(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);
  }

  @Override
  public View newView(final Context context, Cursor cursor, final ViewGroup parent) {
    View v = null;
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    v = inflater.inflate(R.layout.contact_autocomplete_row, parent, false);
    
    return v;
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    
    String[] cols = new String[]{
      ContactsContract.Data.DATA1,
      ContactsContract.Data.DATA2,
      ContactsContract.Data.DATA3,
      ContactsContract.Data.DATA4,
//      ContactsContract.Data.DATA5,
//      ContactsContract.Data.DATA6,
//      ContactsContract.Data.DATA7,
//      ContactsContract.Data.DATA8,
//      ContactsContract.Data.DATA9,
//      ContactsContract.Data.DATA10,
//      ContactsContract.Data.DATA11,
//      ContactsContract.Data.DATA12,
//      ContactsContract.Data.DATA13,
//      ContactsContract.Data.DATA14,
//      ContactsContract.Data.SYNC1,
//      ContactsContract.Data.SYNC2,
//      ContactsContract.Data.SYNC3,
//      ContactsContract.Data.SYNC4,
    };
//    String[] cols = new String[0];
    
    int[] idxes = new int[cols.length];
    
//    String[] colNames = cursor.getColumnNames();
//    String colNamesString = "";
//    for (String s : colNames) {
//      colNamesString += s + ", ";
//    }
    int idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID);
    int nameIdx = cursor.getColumnIndexOrThrow(DISPLAY_NAME);
    int typeIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE);
    
    int i = 0;
    for (String s : cols) {
      idxes[i++] = cursor.getColumnIndexOrThrow(s);
    }
    
    String name = cursor.getString(nameIdx);
    String id = cursor.getString(idIdx);
    String type = cursor.getString(typeIdx);
    Uri photoUri = null;
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      int photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.PHOTO_URI);
      String s = cursor.getString(photoIdx);
      if (s != null) {
        photoUri = Uri.parse(cursor.getString(photoIdx));
      }/* else {
        photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data.PHOTO_THUMBNAIL_URI);
        s = cursor.getString(photoIdx);
        if (s != null) {
          photoUri = Uri.parse(cursor.getString(photoIdx));
        }
      }*/
    } else {
      photoUri = getPhotoUriById(context, (long)Integer.parseInt(id));
    }
//    Log.d("rgai", "photoUri -> " + (photoUri != null ? photoUri.toString() : "null"));
    
    String data = "";
    for (int k : idxes) {
      if (data.length() > 0) {
        data += " - ";
      }
      data += cursor.getString(k);
    }
    
//    if (!allowedMimeTypes.contains(type)) {
//      view.setVisibility(View.GONE);
//      return;
//    }
    
//    String id = cursor.getString(idIdx);
    Log.d("rgai", "photo uri -> " + (photoUri == null ? "null" : photoUri));
//    Log.d("rgai", "THUMBNAIL FOTO URI" + (photoThn == null ? "null" : photoThn));


    Class recipientClass = Settings.getContactDataTypeToRecipientClass().get(type);
    Constructor constructor = null;
    RecipientItem ri = null;
    try {
      constructor = recipientClass.getConstructor(String.class, String.class, Uri.class, int.class);
      ri = (RecipientItem) constructor.newInstance(data, name, photoUri, Integer.parseInt(id));
    } catch (NoSuchMethodException ex) {
      Log.d("rgai", "NoSuchMethodException");
    } catch (InstantiationException ex) {
      Log.d("rgai", "instantiationException");
    } catch (IllegalAccessException ex) {
      Log.d("rgai", "IllegalAccessException");
    } catch (IllegalArgumentException ex) {
      Log.d("rgai", "IllegalArgumentException");
    } catch (InvocationTargetException ex) {
      Log.d("rgai", "InvocationTargetException");
    }
    if (ri != null) {
      ((AutoCompleteRow)view).setRecipient(ri);
    } else {
      Log.d("rgai", "STUCK HERE");
    }
//    ((TestView)view).setPhotoUri(photoUri);
//    ((TestView)view).setUserId(id);
    
    TextView subject = (TextView) view.findViewById(R.id.name);
    subject.setText(name);
    
    TextView from = (TextView) view.findViewById(R.id.data);
    from.setText(data);
    
    TextView date = (TextView) view.findViewById(R.id.type);
    date.setText(type.substring(type.indexOf("/") + 1));
//    date.setText(id);
//    date.setText(photo);
    
    ImageView iv = (ImageView) view.findViewById(R.id.image);
    if (photoUri != null) {
//      photoUri = Uri.withAppendedPath( photoUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY );
      iv.setImageURI(photoUri);
    } else {
      iv.setImageResource(R.drawable.android);
    }
  }
  
  private Uri loadContactPhoto(ContentResolver cr, long id) {
    Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
    Log.d("rgai", "person path -> " + uri.toString());
    Log.d("rgai", "person's photo path -> " + Uri.withAppendedPath(uri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY));
    return Uri.withAppendedPath(uri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
//    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri); 
//    if (input == null) { 
//      return null; 
//    } 
//    return BitmapFactory.decodeStream(input); 
  }

  @Override
  public CharSequence convertToString(Cursor cursor) {
    int nameIdx = cursor.getColumnIndexOrThrow(DISPLAY_NAME);
    String name = cursor.getString(nameIdx);
    return name;
  }
  
  @Override
  public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
    if (constraint == null || constraint.length() < 2) return null;
    String searchString = constraint.toString().toUpperCase();
//    if (searchString.length() < 2) return null;
    
    String[] projection = null;
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      projection = new String[] {
        ContactsContract.Data._ID,
        ContactsContract.Data.CONTACT_ID,
        DISPLAY_NAME,
        ContactsContract.Data.PHOTO_URI,
        ContactsContract.Data.PHOTO_THUMBNAIL_URI,
        ContactsContract.Data.MIMETYPE,
        ContactsContract.Data.DATA1,
        ContactsContract.Data.DATA2,
        ContactsContract.Data.DATA3,
        ContactsContract.Data.DATA4,
        ContactsContract.Data.DATA5
      };
    } else {
      projection = new String[] {
        ContactsContract.Data._ID,
        ContactsContract.Data.CONTACT_ID,
        DISPLAY_NAME,
        ContactsContract.Data.MIMETYPE,
        ContactsContract.Data.DATA1,
        ContactsContract.Data.DATA2,
        ContactsContract.Data.DATA3,
        ContactsContract.Data.DATA4,
        ContactsContract.Data.DATA5,
      };
    }
    
//    String[] projection = null;
    //TODO: Select dataKinds by Settings.java
    String selection = "UPPER(" + DISPLAY_NAME + ") LIKE ? "
            + " AND LENGTH(" + ContactsContract.Data.DATA1 +") != 0 "
            + " AND ("
            + ContactsContract.Data.MIMETYPE + " = '"+ ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE +"'"
            + " OR " + ContactsContract.Data.MIMETYPE + " = '"+ ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE +"'"
//            + " OR " + ContactsContract.Data.MIMETYPE + " = '"+ ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE +"'"
            + ")";
    

    String[] selectionArgs = new String[]{"%"+ searchString +"%"};
    String sort = DISPLAY_NAME;
    
    return mContent.query(ContactsContract.Data.CONTENT_URI,
            projection, selection, selectionArgs, sort);
  }
  
  private static Uri getPhotoUriById(Context context, Long id ) {
    if( id == null || context == null ) return null;

    Uri photo = Uri.parse( "android.resource://"+ context.getPackageName() +"/" + R.drawable.android );
//    Uri photo = null;

//    try {
//        Cursor cursor = context.getContentResolver().query( ContactsContract.Contacts.CONTENT_URI,
//                new String[]{ ContactsContract.Contacts.PHOTO_ID, "photo_uri" },
//                ContactsContract.Contacts._ID + " = " + id + " AND " + ContactsContract.Contacts.PHOTO_ID + " != 0",
//                null,
//                null );
//        if( cursor == null )
//            return photo;
//        if( !cursor.moveToFirst()) {
//            cursor.close();
//            return photo;
//        }
//        String sUri;
//        if(( sUri = cursor.getString( cursor.getColumnIndex( "photo_uri" ))) != null ) {
//            cursor.close();                                                         // content://com.android.contacts/display_photo/1
//            return Uri.parse( sUri );
//        }
//        cursor.close();
//        return photo;
//    } catch( Exception e ) {
        Cursor cursor = context.getContentResolver().query( ContactsContract.Contacts.CONTENT_URI,
                new String[]{ ContactsContract.Contacts.PHOTO_ID },
                ContactsContract.Contacts._ID + " = " + id + " AND " + ContactsContract.Contacts.PHOTO_ID + " != 0",
                null,
                null );
        if( cursor == null )
            return photo;
        if( !cursor.moveToFirst()) {
            cursor.close();
            return photo;
        }
        cursor.close();
        photo = ContentUris.withAppendedId( ContactsContract.Contacts.CONTENT_URI, id );
        photo = Uri.withAppendedPath( photo, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY );
        return photo;
//    }
  }
  
  private static Uri getPhotoUriById2(Context context, Long id ) {
    if( id == null || context == null ) return null;

    Uri photo = Uri.parse( "android.resource://"+ context.getPackageName() +"/" + R.drawable.android );
//    Uri photo = null;
    // api 11 or above
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      photo = Uri.parse("content://com.android.contacts/display_photo/" + id);
//      photo = Uri.withAppendedPath( photo, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY );
    }
    // api 10 or lower
    else {
      photo = ContentUris.withAppendedId( ContactsContract.Contacts.CONTENT_URI, id );
      photo = Uri.withAppendedPath( photo, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY );
    }
//    try {
//        Cursor cursor = context.getContentResolver().query( ContactsContract.Contacts.CONTENT_URI,
//                new String[]{ ContactsContract.Contacts.PHOTO_ID, "photo_uri" },
//                ContactsContract.Contacts._ID + " = " + id + " AND " + ContactsContract.Contacts.PHOTO_ID + " != 0",
//                null,
//                null );
//        if( cursor == null )
//            return photo;
//        if( !cursor.moveToFirst()) {
//            cursor.close();
//            return photo;
//        }
//        String sUri;
//        if(( sUri = cursor.getString( cursor.getColumnIndex( "photo_uri" ))) != null ) {
//            cursor.close();                                                         // content://com.android.contacts/display_photo/1
//            return Uri.parse( sUri );
//        }
//        cursor.close();
//        return photo;
//    } catch( Exception e ) {
//        Cursor cursor = context.getContentResolver().query( ContactsContract.Contacts.CONTENT_URI,
//                new String[]{ ContactsContract.Contacts.PHOTO_ID },
//                ContactsContract.Contacts._ID + " = " + id + " AND " + ContactsContract.Contacts.PHOTO_ID + " != 0",
//                null,
//                null );
//        if( cursor == null )
//            return photo;
//        if( !cursor.moveToFirst()) {
//            cursor.close();
//            return photo;
//        }
//        cursor.close();
//        photo = ContentUris.withAppendedId( ContactsContract.Contacts.CONTENT_URI, id );
//        photo = Uri.withAppendedPath( photo, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY );
//        return photo;
//    }
    return photo;
  }
  
  
}