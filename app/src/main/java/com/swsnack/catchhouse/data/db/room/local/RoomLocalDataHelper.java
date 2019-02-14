package com.swsnack.catchhouse.data.db.room.local;

import android.os.AsyncTask;

import com.swsnack.catchhouse.data.entity.RoomEntity;

import java.util.List;

import androidx.lifecycle.LiveData;

class RoomLocalDataHelper {

    public static class AsyncSetFavoriteRoom extends AsyncTask<RoomEntity, Void, Void> {

        private RoomDao mRoomDao;

        AsyncSetFavoriteRoom(RoomDao roomDao) {
            this.mRoomDao = roomDao;
        }

        @Override
        protected Void doInBackground(RoomEntity... roomEntities) {
            mRoomDao.setFavoriteRoom(roomEntities[0]);
            return null;
        }
    }

    public static class AsyncDeleteFavoriteRoom extends AsyncTask<RoomEntity, Void, Void> {

        private RoomDao mRoomDao;

        AsyncDeleteFavoriteRoom(RoomDao roomDao) {
            mRoomDao = roomDao;
        }

        @Override
        protected Void doInBackground(RoomEntity... roomEntities) {
            mRoomDao.deleteFavoriteRoom(roomEntities[0]);
            return null;
        }
    }

    public static class AsyncLoadFavoriteRoom extends AsyncTask<Void, Void, LiveData<List<RoomEntity>>> {

        private RoomDao mRoomDao;

        public AsyncLoadFavoriteRoom(RoomDao roomDao) {
            this.mRoomDao = roomDao;
        }

        @Override
        protected LiveData<List<RoomEntity>> doInBackground(Void... voids) {
            return mRoomDao.loadFavoriteRoom();
        }
    }
}
