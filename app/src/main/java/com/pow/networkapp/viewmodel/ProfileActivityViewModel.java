package com.pow.networkapp.viewmodel;

import androidx.lifecycle.ViewModel;

import com.pow.networkapp.databinding.ActivityProfileBinding;
import com.pow.networkapp.databinding.ActivityStartBinding;
import com.pow.networkapp.repo.ProfileActivityRepo;

public class ProfileActivityViewModel extends ViewModel {

    private ProfileActivityRepo repo;

    public ProfileActivityViewModel(){
        repo = new ProfileActivityRepo();
    }

    public void getUserInfo(String userId, ActivityProfileBinding binding){
        repo.getUserInfo(userId,binding);
    }

}
