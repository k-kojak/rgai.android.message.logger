package hu.rgai.android.test;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import hu.rgai.android.asynctasks.MessageSender;
import hu.rgai.android.tools.adapter.ThreadViewAdapter;
import hu.rgai.android.config.Settings;
import hu.rgai.android.intent.beens.FacebookRecipientAndr;
import hu.rgai.android.intent.beens.FullThreadMessageParc;
import hu.rgai.android.intent.beens.MessageListElementParc;
import hu.rgai.android.intent.beens.PersonAndr;
import hu.rgai.android.intent.beens.RecipientItem;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.services.ThreadMsgService;
import hu.rgai.android.services.schedulestarters.ThreadMsgScheduler;
import hu.rgai.android.tools.Utils;
import hu.uszeged.inf.rgai.messagelog.beans.Person;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullThreadMessage;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.MessageAtom;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.htmlparser.jericho.Source;

public class ThreadDisplayer extends Activity {

  private ProgressDialog pd = null;
  private Handler handler = null;
  private FullThreadMessageParc content = null;
  private String subject = null;
  private String threadId = "-1";
  private String userId = null;
  private String recipientName = null;
  private AccountAndr account;
  private PersonAndr from;
  private ListView lv = null;
  private EditText text = null;
  private ThreadViewAdapter adapter = null;
  private Set<String> tempMessageIds = null;
  
//  private WebView webView = null;
  private String mailCharCode = "UTF-8";
  
  public static final int MESSAGE_REPLY_REQ_CODE = 1;
  
  private DataUpdateReceiver serviceReceiver;
  private ThreadMsgService service;
  private boolean serviceConnectionEstablished = false;
  private ServiceConnection serviceConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder binder) {
      Log.d("rgai", "# ON ServiceConnected callback");
      service = ((ThreadMsgService.MyBinder) binder).getService();
      service.setAccount(account);
      service.setThreadId(threadId);

//      updateList(service.getEmails());
//      if ((messages == null || !messages.isEmpty()) && pd != null) {
//        pd.dismiss();
//      }
      serviceConnectionEstablished = true;
    }

    public void onServiceDisconnected(ComponentName className) {
      service = null;
    }
  };
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    tempMessageIds = new HashSet<String>();
    MessageListElementParc mlep = (MessageListElementParc)getIntent().getExtras().getParcelable("msg_list_element");
    
    threadId = mlep.getId();
    account = getIntent().getExtras().getParcelable("account");
    subject = mlep.getTitle();
    from = new PersonAndr(mlep.getFrom());
    
    handler = new MessageSendTaskHandler(this);
    
    setContentView(R.layout.threadview_main);
    lv = (ListView) findViewById(R.id.main);
    text = (EditText) findViewById(R.id.text);
    
//    webView = (WebView) findViewById(R.id.email_content);
//    webView.getSettings().setDefaultTextEncodingName(mailCharCode);
    
    
    
    adapter = new ThreadViewAdapter(getApplicationContext(), R.layout.threadview_list_item, account);
    lv.setAdapter(adapter);
    
    if (mlep.getFullMessage() != null) {
      // converting to full thread message, since we MUST use  that here
      content = (FullThreadMessageParc)mlep.getFullMessage();
//      content = getIntent().getExtras().getString("email_content");
//      webView.loadData(content, "text/html", mailCharCode);
//      webView.loadDataWithBaseURL(null, content, "text/html", mailCharCode, null);
      displayMessage();
    } else {
//      handler = new ThreadContentTaskHandler();
//      ThreadContentGetter contentGetter = new ThreadContentGetter(handler, account);
//      contentGetter.execute(threadId);
//
      pd = new ProgressDialog(this);
      pd.setMessage("Fetching content...");
      pd.setCancelable(false);
      pd.show();
    }
  }

  @Override
  protected void onResume() {
    super.onResume(); //To change body of generated methods, choose Tools | Templates.
    
    Intent serviceIntent = new Intent(this, ThreadMsgService.class);
    serviceIntent.putExtra("account", (Parcelable)account);
    serviceIntent.putExtra("threadId", threadId);
    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    
    Intent intent = new Intent(this, ThreadMsgScheduler.class);
    intent.setAction(Settings.Alarms.THREAD_MSG_ALARM_START);
    this.sendBroadcast(intent);
    
    if (serviceReceiver == null) {
      serviceReceiver = new DataUpdateReceiver(this);
    }
    IntentFilter intentFilter = new IntentFilter(Settings.Intents.THREAD_SERVICE_INTENT);
    registerReceiver(serviceReceiver, intentFilter);
  }
  
  public void sendMessage(View view) {
//    List<RecipientItem> to = recipients.getRecipients();
    List<AccountAndr> accs = new LinkedList<AccountAndr>();
    accs.add(account);
    
    RecipientItem ri = new FacebookRecipientAndr("a", from.getId(), from.getName(), null, 1);
    MessageSender rs = new MessageSender(ri, accs, handler, text.getText().toString());
    rs.execute();
    
    String tempId = Utils.generateString(32);
    content.getMessages().add(new MessageAtom(tempId, null, text.getText().toString(), new Date(),
            new Person("1", "me"), true, account.getAccountType(), null));
    displayMessage();
    text.setText("");
    tempMessageIds.add(tempId);
//    }
  }

  @Override
  protected void onPause() {
    super.onPause(); //To change body of generated methods, choose Tools | Templates.
    Log.d("rgai", "ThreadDisplayer onPause");
    if (serviceReceiver != null) {
      unregisterReceiver(serviceReceiver);
    }
    if (serviceConnection != null) {
      unbindService(serviceConnection);
    }
    
    Intent intent = new Intent(this, ThreadMsgScheduler.class);
    intent.setAction(Settings.Alarms.THREAD_MSG_ALARM_STOP);
    this.sendBroadcast(intent);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.message_options_menu, menu);
    return true;
  }
  
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case (MESSAGE_REPLY_REQ_CODE):
        if (resultCode == MessageReply.MESSAGE_SENT_OK) {
          Toast.makeText(this, "Message sent", Toast.LENGTH_LONG).show();
        } else if (resultCode == MessageReply.MESSAGE_SENT_FAILED) {
          Toast.makeText(this, "Failed to send message ", Toast.LENGTH_LONG).show();
        }
        break;
    }
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.message_reply:
        Intent intent = new Intent(this, MessageReply.class);
        Source source = new Source(messageThreadToString(content));
        intent.putExtra("content", source.getRenderer().toString());
        intent.putExtra("subject", subject);
        intent.putExtra("account", (Parcelable)account);
        intent.putExtra("from", from);
        startActivityForResult(intent, MESSAGE_REPLY_REQ_CODE);
        return true;
//        EmailReplySender replySender = new EmailReplySender();
//        replySender.execute();
//        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  
  private String messageThreadToString(FullThreadMessage ftm) {
    StringBuilder sb = new StringBuilder();
    if (ftm != null && ftm.getMessages() != null) {
      for (MessageAtom ma : ftm.getMessages()) {
        sb.append(ma.getContent()).append("<hr/>");
      }
    }
    return sb.toString();
  }

  @Override
  public void finish() {
    Intent resultIntent = new Intent();
    resultIntent.putExtra("message_data", content);
    resultIntent.putExtra("message_id", threadId);

//      if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
      resultIntent.putExtra("account", (Parcelable)account);
//      } else if (account.getAccountType().equals(MessageProvider.Type.GMAIL)) {
//        resultIntent.putExtra("account", new GmailAccountParc((GmailAccount)account));
//      } else if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
//        resultIntent.putExtra("account", new FacebookAccountParc((FacebookAccount)account));
//      }
    setResult(Activity.RESULT_OK, resultIntent);
    super.finish(); //To change body of generated methods, choose Tools | Templates.
  }
  
  private void displayMessage() {
//    String c = "";
//    String mail = from.getEmails().isEmpty() ? "" : " ("+ from.getEmails().get(0) +")";
//    c = from.getName() + mail + "<br/>" + messageThreadToString(content);
//    webView.loadDataWithBaseURL(null, c, "text/html", mailCharCode, null);
    if (content != null) {
      adapter = new ThreadViewAdapter(getApplicationContext(), R.layout.threadview_list_item, account);
      for (MessageAtom ma : content.getMessages()) {
        adapter.add(ma);
      }
      lv.setAdapter(adapter);
      lv.setSelection(lv.getAdapter().getCount() - 1);
    }
  }
  
  private class MessageSendTaskHandler extends Handler {
    
    ThreadDisplayer cont;
    
    public MessageSendTaskHandler(ThreadDisplayer cont) {
      this.cont = cont;
    }
    
    @Override
    public void handleMessage(Message msg) {
      Bundle bundle = msg.getData();
      if (bundle != null) {
        if (bundle.containsKey("success") && bundle.get("success") != null) {
          boolean succ = bundle.getBoolean("success");
          if (succ) {
//            cont.text.setText("");
            Intent intent = new Intent(ThreadDisplayer.this, ThreadMsgScheduler.class);
            intent.setAction(Settings.Alarms.THREAD_MSG_ALARM_START);
            ThreadDisplayer.this.sendBroadcast(intent);
          }
        }
      }
    }
  }
  
  private class DataUpdateReceiver extends BroadcastReceiver {

    private ThreadDisplayer activity;
    
    public DataUpdateReceiver(ThreadDisplayer activity) {
      this.activity = activity;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(Settings.Intents.THREAD_SERVICE_INTENT)) {
        if (intent.getExtras().getInt("result") != ThreadMsgService.OK) {
          String msg = "Error";
          Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
          if (pd != null) {
            pd.dismiss();
          }
        } else {
          FullThreadMessageParc newMessages = intent.getExtras().getParcelable("threadMessage");
          if (content != null) {
            content.getMessages().addAll(newMessages.getMessages());
            if (!tempMessageIds.isEmpty()) {
              for (Iterator<MessageAtom> it = content.getMessages().iterator(); it.hasNext(); ) {
                MessageAtom ma = it.next();
                if (tempMessageIds.contains(ma.getId())) {
                  tempMessageIds.remove(ma.getId());
                  it.remove();
                }
              }
            }
          } else {
            content = newMessages;
          }
//          content = intent.getExtras().getParcelable("threadMessage");
          displayMessage();
//          Parcelable[] messagesParc = intent.getExtras().getParcelableArray("messages");
//          MessageListElementParc[] messages = new MessageListElementParc[messagesParc.length];
//          for (int i = 0; i < messagesParc.length; i++) {
//            messages[i] = (MessageListElementParc) messagesParc[i];
//          }
//
//          updateList(messages);
          if (pd != null) {
            pd.dismiss();
          }
        }
      }
    }
    
//    private FullThreadMessageParc mergeMessages(FullThreadMessageParc newMessages) {
//      
//    }
  }
}
