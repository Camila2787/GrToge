package com.portfolio.lila;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AjustesActivity extends AppCompatActivity {
    private EditText campoNombreCamila;
    private EditText campoBaseCamila;
    private EditText campoBonoCamila;
    private EditText campoNombrePareja;
    private EditText campoBasePareja;
    private TextView tvReglaBono;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        LilaDb db = LilaDb.get(this);
        campoNombreCamila = findViewById(R.id.campoNombreCamila);
        campoBaseCamila = findViewById(R.id.campoBaseCamila);
        campoBonoCamila = findViewById(R.id.campoBonoCamila);
        campoNombrePareja = findViewById(R.id.campoNombrePareja);
        campoBasePareja = findViewById(R.id.campoBasePareja);
        tvReglaBono = findViewById(R.id.tvReglaBono);

        Perfil camila = db.perfil(1);
        Perfil pareja = db.perfil(2);
        campoNombreCamila.setText(camila.nombre);
        campoBaseCamila.setText(String.valueOf(camila.base));
        campoBonoCamila.setText(String.valueOf(camila.bono));
        campoNombrePareja.setText(pareja.nombre);
        campoBasePareja.setText(String.valueOf(pareja.base));
        explicarBono();

        campoBonoCamila.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                explicarBono();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btnAtrasAjustes).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardarAjustes).setOnClickListener(v -> guardar());
    }

    private void explicarBono() {
        long bono = Util.parse(campoBonoCamila.getText());
        tvReglaBono.setText("Cada día de incapacidad descuenta " + Util.cop(Util.bonoDiario(bono))
                + " del bono de la quincena. Si no trabajas ese día, ese bono no entra.");
    }

    private void guardar() {
        String nombreCamila = campoNombreCamila.getText().toString().trim();
        String nombrePareja = campoNombrePareja.getText().toString().trim();
        long baseCamila = Util.parse(campoBaseCamila.getText());
        long bonoCamila = Util.parse(campoBonoCamila.getText());
        long basePareja = Util.parse(campoBasePareja.getText());
        if (nombreCamila.isEmpty()) {
            nombreCamila = "Camila";
        }
        if (nombrePareja.isEmpty()) {
            nombrePareja = "Jhonatan";
        }
        if (baseCamila <= 0 || basePareja <= 0) {
            Toast.makeText(this, "Los salarios base tienen que ser mayores que cero", Toast.LENGTH_SHORT).show();
            return;
        }
        LilaDb db = LilaDb.get(this);
        db.actualizarPerfil(1, nombreCamila, baseCamila, bonoCamila);
        db.actualizarPerfil(2, nombrePareja, basePareja, 0);
        Toast.makeText(this, "Salarios guardados", Toast.LENGTH_SHORT).show();
        finish();
    }
}
