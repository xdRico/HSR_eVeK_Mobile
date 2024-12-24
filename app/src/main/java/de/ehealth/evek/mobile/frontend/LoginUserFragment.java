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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;

import de.ehealth.evek.mobile.core.MainActivity;
import de.ehealth.evek.mobile.exception.UserLoggedInThrowable;
import de.ehealth.evek.mobile.network.DataHandler;
import de.ehealth.evek.mobile.network.IsLoggedInListener;
import de.ehealth.evek.mobile.R;

public class LoginUserFragment extends Fragment implements IsLoggedInListener {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null && getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).setNavigation(false);
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
        String username = tvUser.getText().toString();
        String password = tvPass.getText().toString();
        if(username.isBlank())
            tvUser.setHintTextColor(Color.argb(255, 255, 100, 100));
        if(password.isBlank())
            tvPass.setHintTextColor(Color.argb(255, 255, 100, 100));
        if(username.isBlank() || password.isBlank())
            return;
        DataHandler handler = DataHandler.instance();
        handler.addIsLoggedInListener(this);
        handler.storeNextUser(((CheckBox) getView().findViewById(R.id.cb_login_stay_logged_in)).isChecked());
        handler.tryLogin(username, password);
    }
    @Override
    public void onLoginStateChanged(Throwable loginState) {

        if(getActivity() == null) return;
        if(!(loginState instanceof UserLoggedInThrowable)){
            getActivity().runOnUiThread(() -> {
                if(getActivity() == null){
                    if(getView() == null)
                        return;
                    ((TextView) getView().findViewById(R.id.tv_login_error)).setText(loginState.toString());
                    getView().findViewById(R.id.cl_login_error_box).setVisibility(View.VISIBLE);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Wrong credentials");
                builder.setMessage("Entered credentials do not match!");
                builder.setNegativeButton("Correct", (dialog, which) -> dialog.dismiss()).show();

            });
            return;
        }

        NavController navController = NavHostFragment.findNavController(LoginUserFragment.this);
        NavGraph newNavGraph = switch(DataHandler.instance().getLoginUser().role()) {
            case HealthcareDoctor, TransportDoctor, SuperUser ->
                navController.getNavInflater().inflate(R.navigation.nav_graph_doctor);
            case TransportUser ->
                navController.getNavInflater().inflate(R.navigation.nav_graph_user);
            default -> throw new RuntimeException("Invalid user Role - how did we get here??");
        };

        getActivity().runOnUiThread(() -> navController.setGraph(newNavGraph));
    }
}
