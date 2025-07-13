package com.example.coreteamproject

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class DiaryFragment : Fragment() {

    // Inizializza Firestore come dice il prof
    private val db = Firebase.firestore

    private lateinit var textViewDiary: TextView
    private lateinit var btnAggiungiValutazione: Button
    private lateinit var layoutValutazione: LinearLayout
    private lateinit var seekBarStress: SeekBar
    private lateinit var seekBarColleghi: SeekBar
    private lateinit var seekBarSoddisfazione: SeekBar
    private lateinit var editCommento: EditText
    private lateinit var btnSalva: Button
    private lateinit var btnAnnulla: Button
    private lateinit var textStressValue: TextView
    private lateinit var textColleghiValue: TextView
    private lateinit var textSoddisfazioneValue: TextView
    private lateinit var recyclerViewValutazioni: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_diary, container, false)

        // Collega i campi
        textViewDiary = view.findViewById(R.id.textViewDiary)
        btnAggiungiValutazione = view.findViewById(R.id.btn_aggiungi_valutazione)
        layoutValutazione = view.findViewById(R.id.layout_valutazione)
        seekBarStress = view.findViewById(R.id.seekbar_stress)
        seekBarColleghi = view.findViewById(R.id.seekbar_colleghi)
        seekBarSoddisfazione = view.findViewById(R.id.seekbar_soddisfazione)
        editCommento = view.findViewById(R.id.edit_commento)
        btnSalva = view.findViewById(R.id.btn_salva)
        btnAnnulla = view.findViewById(R.id.btn_annulla)
        textStressValue = view.findViewById(R.id.text_stress_value)
        textColleghiValue = view.findViewById(R.id.text_colleghi_value)
        textSoddisfazioneValue = view.findViewById(R.id.text_soddisfazione_value)
        recyclerViewValutazioni = view.findViewById(R.id.recycler_valutazioni)

        // Imposta i listener per i SeekBar
        impostaSeekBarListeners()

        // Pulsante + per aggiungere valutazione
        btnAggiungiValutazione.setOnClickListener {
            if (layoutValutazione.visibility == View.GONE) {
                mostraFormValutazione()
            }
        }

        // Pulsante Salva
        btnSalva.setOnClickListener {
            salvaValutazione()
        }

        // Pulsante Annulla
        btnAnnulla.setOnClickListener {
            nascondiFormValutazione()
        }

        // Carica le valutazioni esistenti
        caricaValutazioni()

        return view
    }

    private fun impostaSeekBarListeners() {
        seekBarStress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textStressValue.text = "${progress + 1}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarColleghi.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textColleghiValue.text = "${progress + 1}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarSoddisfazione.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textSoddisfazioneValue.text = "${progress + 1}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun mostraFormValutazione() {
        layoutValutazione.visibility = View.VISIBLE
        btnAggiungiValutazione.visibility = View.GONE

        // Reset dei valori
        seekBarStress.progress = 2 // valore default 3
        seekBarColleghi.progress = 2
        seekBarSoddisfazione.progress = 2
        editCommento.text.clear()
    }

    private fun nascondiFormValutazione() {
        layoutValutazione.visibility = View.GONE
        btnAggiungiValutazione.visibility = View.VISIBLE
    }

    private fun salvaValutazione() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val stressValue = seekBarStress.progress + 1
            val colleghiValue = seekBarColleghi.progress + 1
            val soddisfazioneValue = seekBarSoddisfazione.progress + 1
            val commento = editCommento.text.toString()

            // Crea la data del mese corrente
            val calendar = Calendar.getInstance()
            val monthYear = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(calendar.time)
            val fullDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)

            // Crea un oggetto con i dati come nell'esempio del prof
            val valutazioneMensile = hashMapOf(
                "stress" to stressValue,
                "rapportoColleghi" to colleghiValue,
                "soddisfazioneLavoro" to soddisfazioneValue,
                "commento" to commento,
                "meseAnno" to monthYear,
                "dataCompleta" to fullDate,
                "userId" to user.uid,
                "timestamp" to System.currentTimeMillis()
            )

            // Salva usando il metodo set() come dice il prof
            db.collection("ValutazioniMensili")
                .document("${user.uid}_$monthYear")
                .set(valutazioneMensile)
                .addOnSuccessListener {
                    Log.d("DiaryFragment", "Valutazione salvata con successo!")
                    Toast.makeText(requireContext(), "Valutazione salvata!", Toast.LENGTH_SHORT).show()
                    nascondiFormValutazione()
                    caricaValutazioni()
                }
                .addOnFailureListener { e ->
                    Log.w("DiaryFragment", "Errore nel salvataggio", e)
                    Toast.makeText(requireContext(), "Errore nel salvataggio", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun caricaValutazioni() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Leggi i dati come dice il prof
            db.collection("ValutazioniMensili")
                .whereEqualTo("userId", user.uid)
                .get()
                .addOnSuccessListener { documents ->
                    recyclerViewValutazioni.removeAllViews()

                    if (documents.isEmpty) {
                        val textNoData = TextView(requireContext())
                        textNoData.text = "Nessuna valutazione trovata. Aggiungi la prima!"
                        textNoData.textSize = 16f
                        textNoData.setPadding(16, 16, 16, 16)
                        recyclerViewValutazioni.addView(textNoData)
                    } else {
                        for (document in documents) {
                            Log.d("DiaryFragment", "Valutazione: ${document.data}")
                            aggiungiValutazioneAllaLista(document.data)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("DiaryFragment", "Errore nel leggere i dati", exception)
                }
        }
    }

    private fun aggiungiValutazioneAllaLista(data: Map<String, Any>?) {
        if (data == null) return

        // Crea il container principale
        val containerLayout = LinearLayout(requireContext())
        containerLayout.orientation = LinearLayout.VERTICAL
        containerLayout.setPadding(16, 16, 16, 16)
        containerLayout.setBackgroundColor(0xFFF5F5F5.toInt()) // grigio chiaro

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, 16) // margine sotto
        containerLayout.layoutParams = layoutParams

        // Titolo mese/anno
        val textMeseAnno = TextView(requireContext())
        textMeseAnno.text = "Mese: ${data["meseAnno"]}"
        textMeseAnno.textSize = 18f
        textMeseAnno.setTypeface(null, android.graphics.Typeface.BOLD)
        textMeseAnno.setTextColor(resources.getColor(R.color.purple_500, null))
        textMeseAnno.setPadding(0, 0, 0, 16)
        containerLayout.addView(textMeseAnno)

        // Stress
        val textStress = TextView(requireContext())
        textStress.text = "Stress: ${data["stress"]}/5"
        textStress.textSize = 14f
        textStress.setPadding(0, 0, 0, 8)
        containerLayout.addView(textStress)

        // Colleghi
        val textColleghi = TextView(requireContext())
        textColleghi.text = "Colleghi: ${data["rapportoColleghi"]}/5"
        textColleghi.textSize = 14f
        textColleghi.setPadding(0, 0, 0, 8)
        containerLayout.addView(textColleghi)

        // Soddisfazione
        val textSoddisfazione = TextView(requireContext())
        textSoddisfazione.text = "Soddisfazione: ${data["soddisfazioneLavoro"]}/5"
        textSoddisfazione.textSize = 14f
        textSoddisfazione.setPadding(0, 0, 0, 8)
        containerLayout.addView(textSoddisfazione)

        // Commento (solo se presente)
        val commento = data["commento"] as? String
        if (!commento.isNullOrEmpty()) {
            val textCommento = TextView(requireContext())
            textCommento.text = "Commento: $commento"
            textCommento.textSize = 14f
            textCommento.setTypeface(null, android.graphics.Typeface.ITALIC)
            containerLayout.addView(textCommento)
        }

        recyclerViewValutazioni.addView(containerLayout)
    }
}