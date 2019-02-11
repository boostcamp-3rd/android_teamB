package com.swsnack.catchhouse.data.chattingdata.remote;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.swsnack.catchhouse.data.chattingdata.ChattingManager;
import com.swsnack.catchhouse.data.chattingdata.model.Chatting;
import com.swsnack.catchhouse.data.chattingdata.model.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.swsnack.catchhouse.Constant.Chatting.NO_CHAT_ROOM;
import static com.swsnack.catchhouse.Constant.ExceptionReason.NOT_SIGNED_USER;
import static com.swsnack.catchhouse.Constant.FirebaseKey.CHATTING;
import static com.swsnack.catchhouse.Constant.FirebaseKey.DB_USER;
import static com.swsnack.catchhouse.Constant.FirebaseKey.MESSAGE;

public class RemoteChattingManager implements ChattingManager {

    private DatabaseReference db;

    private static RemoteChattingManager INSTANCE;

    public static synchronized RemoteChattingManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RemoteChattingManager();
        }
        return INSTANCE;
    }

    private RemoteChattingManager() {
        this.db = FirebaseDatabase.getInstance().getReference().child(CHATTING);
    }

    @Override
    public void getChattingRoom(@NonNull String uuid,
                                @NonNull String destinationUuid,
                                @NonNull OnSuccessListener<String> onSuccessListener,
                                @NonNull OnFailureListener onFailureListener) {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            onFailureListener.onFailure(new RuntimeException(NOT_SIGNED_USER));
            return;
        }

        db.orderByChild(DB_USER + "/" + uuid)
                .equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() == null) {
                            onSuccessListener.onSuccess(NO_CHAT_ROOM);
                            return;
                        }
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Chatting chatting = snapshot.getValue(Chatting.class);
                            if (chatting.getUsers().containsKey(destinationUuid)) {
                                onSuccessListener.onSuccess(snapshot.getKey());
                                return;
                            }
                            onSuccessListener.onSuccess(NO_CHAT_ROOM);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        onFailureListener.onFailure(databaseError.toException());
                    }
                });
    }

    @Override
    public void getChattingList(@NonNull String uuid, @NonNull OnSuccessListener<List<Chatting>> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            onFailureListener.onFailure(new RuntimeException(NOT_SIGNED_USER));
            return;
        }

        db.orderByChild(DB_USER + "/" + uuid)
                .equalTo(true)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Chatting> chattingList = new ArrayList<>();
                        if (dataSnapshot.getValue() == null) {
                            onSuccessListener.onSuccess(chattingList);
                            return;
                        }
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Chatting chatting = snapshot.getValue(Chatting.class);
                            chatting.setRoomUid(snapshot.getKey());
                            chattingList.add(chatting);
                        }
                        onSuccessListener.onSuccess(chattingList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        onFailureListener.onFailure(databaseError.toException());
                    }
                });
    }

    @Override
    public void getChatMessage(@NonNull String chatRoomId,
                               @NonNull OnSuccessListener<Map<String, Message>> onSuccessListener,
                               @NonNull OnFailureListener onFailureListener) {
        db.child(chatRoomId)
                .child(MESSAGE)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        if (dataSnapshot.getValue() != null) {
                            Map<String, Message> newMessage = new HashMap<>();
                            newMessage.put(dataSnapshot.getKey(), dataSnapshot.getValue(Message.class));
                            onSuccessListener.onSuccess(newMessage);
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        //채팅 메세지 수정기능을 추가하게 될 시, 구현
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                        //채팅 메세지 수정기능을 추가하게 될 시, 구현
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        //채팅 메세지 수정기능을 추가하게 될 시, 구현
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        onFailureListener.onFailure(databaseError.toException());
                    }
                });
    }

    @Override
    public void setChattingRoom(@NonNull Chatting chattingUser, @NonNull OnSuccessListener<Void> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        db
                .push()
                .setValue(chattingUser)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener);
    }

    @Override
    public void setChatMessage(@NonNull Message message,
                               @NonNull OnSuccessListener<Void> onSuccessListener,
                               @NonNull OnFailureListener onFailureListener) {

    }
}
