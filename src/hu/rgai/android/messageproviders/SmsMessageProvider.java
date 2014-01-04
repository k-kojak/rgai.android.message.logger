package hu.rgai.android.messageproviders;

import android.content.BroadcastReceiver;
import hu.rgai.android.intent.beens.SmsMessageRecipientAndr;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.net.ssl.SSLHandshakeException;

import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.MessageListElement;
import hu.uszeged.inf.rgai.messagelog.beans.MessageRecipient;
import hu.uszeged.inf.rgai.messagelog.beans.Person;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullThreadMessage;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import hu.rgai.android.config.Settings;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.MessageAtom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SmsMessageProvider extends BroadcastReceiver implements MessageProvider {

  Context context;

  public SmsMessageProvider(){};
  
  public SmsMessageProvider(Context myContext) {
    context = myContext;
  }

  @Override
  public List<MessageListElement> getMessageList(int offset, int limit)
          throws CertPathValidatorException, SSLHandshakeException,
          ConnectException, NoSuchProviderException, UnknownHostException,
          IOException, MessagingException, AuthenticationFailedException {

    final List<MessageListElement> messages = new LinkedList<MessageListElement>();
    int foundThreads = 0;
    
    Uri uriSMSURI = Uri.parse("content://sms");
    Cursor cur = context.getContentResolver().query(uriSMSURI,
            new String[]{"thread_id", "body", "date", "seen", "person", "address", "type"},
            null,
            null,
            "date DESC");
    while (cur.moveToNext()) {
      String title = cur.getString(1);
      if (title.length() > Settings.MAX_SNIPPET_LENGTH) {
        title = title.substring(0, Settings.MAX_SNIPPET_LENGTH) + "...";
      }
      boolean seen = cur.getInt(3) == 1;
      boolean isMe = cur.getInt(6) == 2;
      MessageItem ti = new MessageItem(cur.getString(0), title, seen, isMe, cur.getLong(4),
              cur.getString(5), cur.getLong(2));
//      Log.d("rgai", "MessageItem -> " + ti);
      boolean contains = false;
      int containIndex = -1;
      for (MessageListElement mle : messages) {
        containIndex++;
        if (mle.getId().equals(ti.threadId)) {
          contains = true;
          break;
        }
      }
      
      Person from = null;
//      if (!ti.isMe) {
        from = new Person(ti.personId+"", ti.address, Type.SMS);
//      } else {
////        from = new Person(ti.personId+"", ti.address, Type.SMS);
//      }
      if (contains) {
        MessageListElement mle = messages.get(containIndex);
        if (!ti.isMe && !from.getId().equals("0")) {
          mle.setFrom(from);
        }
        if (!ti.seen) {
          mle.setSeen(false);
        }
      } else {
        foundThreads++;
        if (foundThreads > limit) break;
        messages.add(new MessageListElement(ti.threadId, ti.seen, ti.title, from,
                new Date(ti.date), Type.SMS));
      }
//      if ()
//      Log.d("rgai", ti.toString());
    }

//    uriSMSURI = Uri.parse("content://sms");
//    cur = context.getContentResolver().query(uriSMSURI,
//            new String[]{"thread_id", "body", "date", "seen", "person", "address"},
//            null,
//            null,
//            "date DESC LIMIT " + limit);


//    while (cur.moveToNext()) {
//
//      Log.d("rgai", cur.getString(2));
//      messages.add(new MessageListElement(
//              cur.getString(0),
//              true,
//              cur.getString(1),
//              1,
//              new Person(cur.getLong(4) + "", cur.getString(5), MessageProvider.Type.SMS),
//              new Date(Long.parseLong(cur.getString(2))),
//              MessageProvider.Type.SMS));
//    }
//    Log.d("rgai", messages.toString());
    return messages;


  }

  @Override
  public FullThreadMessage getMessage(String threadId )throws NoSuchProviderException,
          MessagingException, IOException {
    // TODO Auto-generated method stub

    final FullThreadMessage ftm = new FullThreadMessage();
    String selection = "thread_id = " + threadId;

    Uri uriSMSURI = Uri.parse("content://sms");
    Cursor cur = context.getContentResolver().query(uriSMSURI,
            new String[]{"thread_id", "_id", "subject", "body", "date", "person", "address", "type"},
            selection, null, null);

    /**
     * 0: _id 1: thread_id 2: address 3: person 4: date 5: date_sent 6: protocol 7: read
     * 8: status 9: type 10: reply_path_present 11: subject 12: body 13: service_center
     * 14: locked 15: error_code 16: seen
     */
    while (cur.moveToNext()) {
      if (cur.getString(0).equals(threadId)) {

//        Log.d("rgai", "SMS DATE -> " + cur.getString(4));
//        Log.d("rgai", "SMS ADDRESS -> " + cur.getString(2));
//        Log.d("rgai", "SMS PERSON -> " + cur.getLong(3));
//        Log.d("rgai", "SMS READ -> " + cur.getLong(7));
//        Log.d("rgai", "SMS STATUS -> " + cur.getLong(8));
//        Log.d("rgai", "SMS TYPE -> " + cur.getLong(9));
//        Log.d("rgai", "SMS SEEN -> " + cur.getLong(16));
        ftm.addMessage(new MessageAtom(
                cur.getString(1),
                cur.getString(2),
                cur.getString(3),
                new Date(cur.getLong(4)),
                new Person(cur.getLong(5) + "", cur.getString(6), MessageProvider.Type.SMS),
                cur.getLong(7) == 2, //vmit ezzel kezdeni
                MessageProvider.Type.SMS,
                null));

      }
    }

    return ftm;
  }

  @Override
  public void sendMessage(Set<? extends MessageRecipient> to, String content,
          String subject) throws NoSuchProviderException, MessagingException,
          IOException {
    // TODO Auto-generated method stub

    for (MessageRecipient mr : to) {

      SmsMessageRecipientAndr smr = (SmsMessageRecipientAndr) mr;

      SmsManager smsman = SmsManager.getDefault();
      smsman.sendTextMessage(smr.getData(), null, content, null, null);


      ContentValues sentSms = new ContentValues();
      sentSms.put("address", smr.getData());
      sentSms.put("body", content);



      ContentResolver contentResolver = context.getContentResolver();
      Uri uri = Uri.parse("content://sms/sent");
      contentResolver.insert(uri, sentSms);
    }

  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
      // sms broadcast arrives earlier than sms actually stored in inbox, we have to delay
      // a bit the reading from inbox
      try {
        Thread.sleep(750);
      } catch (InterruptedException ex) {
        Logger.getLogger(SmsMessageProvider.class.getName()).log(Level.SEVERE, null, ex);
      }
      Intent res = new Intent(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST);
      res.putExtra("type", MessageProvider.Type.SMS.toString());
      context.sendBroadcast(res);
      
//      // in case the first attempt was too quick, request the display again a little bit later
//      try {
//        Thread.sleep(2000);
//      } catch (InterruptedException ex) {
//        Logger.getLogger(SmsMessageProvider.class.getName()).log(Level.SEVERE, null, ex);
//      }
//      res = new Intent(Settings.Intents.NEW_MESSAGE_ARRIVED_BROADCAST);
//      context.sendBroadcast(res);
    }
  }

  private class MessageItem {
    private String threadId;
    private String title;
    private boolean seen;
    private boolean isMe;
    private long personId;
    private String address;
    private long date;

    public MessageItem(String threadId, String title, boolean seen, boolean isMe, long personId, String address, long date) {
      this.threadId = threadId;
      this.title = title;
      this.seen = seen;
      this.isMe = isMe;
      this.personId = personId;
      this.address = address;
      this.date = date;
    }

    @Override
    public String toString() {
      return "MessageItem{" + "threadId=" + threadId + ", title=" + title + ", seen=" + seen + ", isMe=" + isMe + ", personId=" + personId + ", address=" + address + ", date=" + date + '}';
    }

  }
  
}
