package de.ehealth.evek.mobile.frontend;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import de.ehealth.evek.mobile.core.MainActivity;
import de.ehealth.evek.mobile.exception.UserLoggedInThrowable;
import de.ehealth.evek.mobile.network.DataHandler;
import de.ehealth.evek.mobile.network.IsLoggedInListener;
import de.ehealth.evek.mobile.R;

public class LoginUserFragment extends Fragment implements IsLoggedInListener {

    private String username = null;
    private String password = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() != null)
            ((MainActivity) getActivity()).setNavigationElementsVisible(false);

    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //TODO Read stored login data
        if (username != null && password != null
            && !username.isBlank() && !password.isBlank()){
            /*TODO assign stored login Data
                this.username = username;
                this.password = password;*/
            tryLogin();
        }

        View view = inflater.inflate(R.layout.fragment_login_user, container, false);
        // Inflate the layout for this fragment
        view.findViewById(R.id.btn_login_login).setOnClickListener(v -> tryLogin());
        view.findViewById(R.id.ib_login_view_password).setOnTouchListener((v, event) -> {
            boolean ret;
            EditText pw = view.findViewById(R.id.tb_login_pass);
            ret = switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    pw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    yield true;
                }
                case MotionEvent.ACTION_UP -> {
                    pw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    yield true;
                }
                default -> false;
            };
            view.performClick();
            return ret;
        });

        return view;
    }
    private void tryLogin() {
        if(getView() == null)
            return;
        TextView tvUser = getView().findViewById(R.id.tb_login_user);
        TextView tvPass = getView().findViewById(R.id.tb_login_pass);
        username = tvUser.getText().toString();
        password = tvPass.getText().toString();
        if(username.isBlank())
            tvUser.setHintTextColor(Color.argb(255, 255, 100, 100));
        if(password.isBlank())
            tvPass.setHintTextColor(Color.argb(255, 255, 100, 100));
        if(username.isBlank() || password.isBlank())
            return;
        DataHandler handler = DataHandler.instance();
        handler.addIsLoggedInListener(this);
        handler.tryLogin(username, password);
    }
    @Override
    public void onLoginStateChanged(Throwable loginState) {

            if(getActivity() == null) return;
            if(!(loginState instanceof UserLoggedInThrowable)){
            getActivity().runOnUiThread(() -> {
                if(getView() == null)
                    return;
                ((TextView) getView().findViewById(R.id.tv_login_error)).setText(loginState.toString());
                getView().findViewById(R.id.cl_login_error_box).setVisibility(View.VISIBLE);
            });
            return;
        }

        if(getView() != null
                && ((CheckBox) getView().findViewById(R.id.cb_login_stay_logged_in)).isChecked()){
            //TODO store Login Data
        }

        NavController navController = NavHostFragment.findNavController(LoginUserFragment.this);
        if(navController.getCurrentDestination() == null
                || navController.getCurrentDestination().getId() != R.id.loginUserFragment) return;
        getActivity().runOnUiThread(() -> navController.navigate(R.id.action_loginUserFragment_to_mainPageFragment));
    }
}
