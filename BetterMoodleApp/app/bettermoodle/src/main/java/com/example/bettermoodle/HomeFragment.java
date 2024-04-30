package com.example.bettermoodle;

import static com.example.bettermoodle.UtilsKt.b64ToBitmap;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

public class HomeFragment extends Fragment {

    CardView StudentInfoCard;
    CardView RecentCard;
    CardView AssignmentsCard;
    CardView CoursesCard;
    JsonInterface jsonInterface;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.jsonInterface = new JsonInterface(context, "student_info");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView userName = view.findViewById(R.id.home_welcome);
        ImageView userAvatar = view.findViewById(R.id.def_user_photo);
        this.jsonInterface.postResponseToListener().observe(getViewLifecycleOwner(), s -> {
            try {
                JSONObject response = new JSONObject(s);
                String name = response.getString("name");
                String image = response.getString("image");
//                byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
//                Bitmap decodedBytes = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                userName.setText(name);
                userAvatar.setImageBitmap(b64ToBitmap(image));
            } catch (JSONException e) {

                throw new RuntimeException(e);
            }
        });

    }
}