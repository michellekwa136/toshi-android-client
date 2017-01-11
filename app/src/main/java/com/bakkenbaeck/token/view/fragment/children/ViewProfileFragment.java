package com.bakkenbaeck.token.view.fragment.children;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bakkenbaeck.token.R;
import com.bakkenbaeck.token.databinding.FragmentViewProfileBinding;
import com.bakkenbaeck.token.presenter.ProfilePresenter;
import com.bakkenbaeck.token.presenter.ViewProfilePresenter;
import com.bakkenbaeck.token.presenter.factory.PresenterFactory;
import com.bakkenbaeck.token.presenter.factory.ViewProfilePresenterFactory;
import com.bakkenbaeck.token.view.fragment.BasePresenterFragment;

public class ViewProfileFragment extends BasePresenterFragment<ViewProfilePresenter, ViewProfileFragment> {

    private FragmentViewProfileBinding binding;
    private ProfilePresenter.OnEditButtonListener onEditButtonListener;
    private ViewProfilePresenter presenter;

    public static ViewProfileFragment newInstance() {
        return new ViewProfileFragment();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle inState) {
        this.binding = DataBindingUtil.inflate(inflater, R.layout.fragment_view_profile, container, false);
        return binding.getRoot();
    }

    public FragmentViewProfileBinding getBinding() {
        return this.binding;
    }

    @NonNull
    @Override
    protected PresenterFactory<ViewProfilePresenter> getPresenterFactory() {
        return new ViewProfilePresenterFactory();
    }

    @Override
    protected void onPresenterPrepared(@NonNull final ViewProfilePresenter presenter) {
        this.presenter = presenter;
        if (this.onEditButtonListener != null) {
            this.presenter.setOnEditButtonListener(this.onEditButtonListener);
        }
    }

    @Override
    protected int loaderId() {
        return 5002;
    }

    public void setOnEditButtonListener(final ProfilePresenter.OnEditButtonListener onEditButtonListener) {
        this.onEditButtonListener = onEditButtonListener;
        if (this.presenter == null) {
            return;
        }
        this.presenter.setOnEditButtonListener(this.onEditButtonListener);
    }
}