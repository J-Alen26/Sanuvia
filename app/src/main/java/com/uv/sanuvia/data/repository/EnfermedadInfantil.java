package com.uv.sanuvia.data.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EnfermedadInfantil {

    private static final String COLLECTION = "enfermedades_infantiles";
    private final FirebaseFirestore db;

    public EnfermedadInfantil() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Recupera una vez la lista de enfermedades infantiles desde Firestore.
     */
    public LiveData<List<EnfermedadInfantilModel>> getEnfermedadesInfantiles() {
        MutableLiveData<List<EnfermedadInfantilModel>> liveData = new MutableLiveData<>();
        db.collection(COLLECTION)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<EnfermedadInfantilModel> list = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    EnfermedadInfantilModel item = doc.toObject(EnfermedadInfantilModel.class);
                    list.add(item);
                }
                liveData.setValue(list);
            })
            .addOnFailureListener(e -> {
                liveData.setValue(null);
            });
        return liveData;
    }

    /**
     * Escucha en tiempo real los cambios en la colección de enfermedades infantiles.
     * @param onUpdate Callback con la lista actualizada.
     * @param onError Callback en caso de error.
     */
    public ListenerRegistration listenEnfermedadesInfantiles(
            Consumer<List<EnfermedadInfantilModel>> onUpdate,
            Consumer<FirebaseFirestoreException> onError) {
        return db.collection(COLLECTION)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    onError.accept(error);
                    return;
                }
                List<EnfermedadInfantilModel> list = new ArrayList<>();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    EnfermedadInfantilModel item = doc.toObject(EnfermedadInfantilModel.class);
                    list.add(item);
                }
                onUpdate.accept(list);
            });
    }

    /**
     * Modelo de datos para mapear documentos de Firestore.
     */
    public static class EnfermedadInfantilModel {
        private String titulo;
        private String descripcion;
        private String fecha;

        private String UrlImage;

        public EnfermedadInfantilModel() { }



        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public String getUrlImage() { return UrlImage; }
        public void setFecha(String fecha) { this.fecha = fecha; }
    }
}
