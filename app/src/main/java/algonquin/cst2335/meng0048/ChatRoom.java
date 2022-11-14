package algonquin.cst2335.meng0048;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import algonquin.cst2335.meng0048.databinding.ActivityChatRoomBinding;
import algonquin.cst2335.meng0048.databinding.SentMessageBinding;

public class ChatRoom extends AppCompatActivity {

    ChatRoomViewModel chatModel;
    ArrayList<ChatMessage> allMessages ;
    ActivityChatRoomBinding binding;
    private RecyclerView.Adapter<MyRowHolder> myAdapter;
    ChatMessageDAO mDAO;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChatRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MessageDatabase db = Room.databaseBuilder(getApplicationContext(), MessageDatabase.class,
                "MessageDatabase").build();
        mDAO = db.cmDAO();


        // get values from ChatRoomViewModel and assign the values to allMessages
        chatModel = new ViewModelProvider(this).get(ChatRoomViewModel.class);
        allMessages = chatModel.messages.getValue();
        // initiate allMessages if the messages declared in ChatRoomViewModel is null
        if(allMessages == null)
        {
            chatModel.messages.postValue( allMessages = new ArrayList<ChatMessage>() );
            Executor thread = Executors.newSingleThreadExecutor();
            thread.execute( () ->{
                allMessages.addAll(mDAO.getAllMessages());
                // load RecycleView
                runOnUiThread( () ->{
                    binding.recycleView.setAdapter(myAdapter );
                });
            });

        }
        chatModel.selectedMessage.observe(this, (newMessageValue) -> {

            MessageDetailsFragment  chatFragment = new MessageDetailsFragment( newMessageValue );
            FragmentManager fMgr = getSupportFragmentManager();
            FragmentTransaction tx = fMgr.beginTransaction();
            tx.add(R.id.fragmentLocation,chatFragment);
            tx.addToBackStack("Back to previous activity");
            tx.commit();//This line actually loads the fragment into the specified FrameLayout

            //build pattern
            /*
               getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentLocation,chatFragment)
                    .commit();
             */

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentLocation, chatFragment)
                    .commit();




        });

        //set a layout manager for the rows to be aligned vertically using only 1 column
        binding.recycleView.setLayoutManager( new LinearLayoutManager(this));

        //SendButton: add the message typed in the edittext and time to the allMessages
        binding.sendButton.setOnClickListener( click -> {

            String message = binding.textInput.getText().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd-MMM-yyyy hh-mm-ss a");
            String currentDateandTime = sdf.format( new Date());

            ChatMessage sendMessage ;
            sendMessage = new ChatMessage( message, currentDateandTime,true);
            chatModel.messages.getValue().add(sendMessage);
//            allMessages.add( sendMessage);
            myAdapter.notifyItemInserted(allMessages.size()-1);
            binding.textInput.setText("");

            Executor thread = Executors.newSingleThreadExecutor();
            thread.execute( () ->{
                 try{
                     long last = mDAO.insertMessage(sendMessage);
                     sendMessage.id =(int) last;
                 }catch (Exception e ){
                     Log.e("Database", e.getMessage());
                 }
             });
        });

        //ReceiveButton
        binding.receiveButton.setOnClickListener( click -> {

            ChatMessage receiveMessage ;
            String message = binding.textInput.getText().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd-MMM-yyyy hh-mm-ss a");
            String currentDateandTime = sdf.format( new Date());

            receiveMessage = new ChatMessage( message, currentDateandTime,false);
//            allMessages.add( receiveMessage);
            chatModel.messages.getValue().add(receiveMessage);
            myAdapter.notifyItemInserted(allMessages.size()-1);
            binding.textInput.setText("");

            Executor thread = Executors.newSingleThreadExecutor();
            thread.execute( () ->{
                // insert newMessage into database

                try{
                    long last = mDAO.insertMessage(receiveMessage);
                    receiveMessage.id =(int) last;
                }catch (Exception e ){
                    Log.e("Database", e.getMessage());
                }
            });
        });

        binding.recycleView.setAdapter( myAdapter = new RecyclerView.Adapter<MyRowHolder>() {
            @NonNull
            @Override
            public MyRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View root;
                if(viewType == 0)
                    root = getLayoutInflater().inflate(R.layout.sent_message, parent, false);
                else
                    root = getLayoutInflater().inflate( R.layout.receive_message, parent, false);

                return new MyRowHolder( root );
            }

            @Override
            public void onBindViewHolder(@NonNull MyRowHolder holder, int position) {
                ChatMessage object = allMessages.get(position);
                holder.messageText.setText(object.getMessage());
                holder.timeText.setText(object.getTimeSent());

            }

            @Override
            public int getItemCount() {

                return allMessages.size();
            }

            @Override
            public int getItemViewType(int position) {
                ChatMessage object = allMessages.get(position);

                if(object.getIsSentButton()==true) // if(object.getIsSentButton() == true)
                    return 0; //0 represents send, text on the left
                else
                    return 1;//1 represents receive, text on the right
            }
        });
    }


    class MyRowHolder extends RecyclerView.ViewHolder{
        TextView messageText;
        TextView timeText;
        public MyRowHolder(@NonNull View itemView){
            super(itemView);
            messageText = itemView.findViewById(R.id.message);
            timeText = itemView.findViewById(R.id.time);

            itemView.setOnClickListener( click ->{

                  //which row was click
                int position = getAbsoluteAdapterPosition();
                ChatMessage selected = allMessages.get(position);
                chatModel.selectedMessage.postValue( selected );


                /* AlertDialog.Builder builder = new AlertDialog.Builder( ChatRoom.this);
                // method with build pattern
                builder.setMessage(messageText.getText())
                        .setTitle("Do you want to delete this message?")
                        .setNegativeButton("No", (dialog, cl) ->{ })
                        .setPositiveButton("Yes", (dialog, cl) ->{

                            Executor thread = Executors.newSingleThreadExecutor();
                            thread.execute( () ->{
                                mDAO.deleteMessage(thisMessage);

                            });

                            allMessages.remove(thisMessage);
                            myAdapter.notifyItemRemoved(position);

                            Snackbar.make(messageText,"You deleted position #" + position,
                                    Snackbar.LENGTH_LONG).setAction("Undo",clk->{
//                                        Executor thread = Executors.newSingleThreadExecutor();
                                        thread.execute(()->{
                                            mDAO.insertMessage(thisMessage);
                                         });
        //                                allMessages.add(position, thisMessage);
                                        chatModel.messages.getValue().add(position,thisMessage);
                                        myAdapter.notifyItemInserted(position);
                                 }).show();
                        }).create().show();*/






            });
        }

    }

}
