package com.craftmaster2190.automyqhubspace

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.craftmaster2190.automyqhubspace.databinding.FragmentGarageBinding
import com.craftmaster2190.automyqhubspace.ui.login.AppCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class GarageFragment : Fragment() {
    private val myqClient by lazy {
        MyQClient.instance.also { myqClient_ ->
            AppCredentials.fromContext(this.requireContext())?.also {
                myqClient_.username = it.myqUsername
                myqClient_.password = it.myqPassword
            }
        }
    }
    private val updater = Executors.newSingleThreadScheduledExecutor()
    private var schedule: ScheduledFuture<*>? = null

    private var binding: FragmentGarageBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGarageBinding.inflate(inflater, container, false)
        binding!!.lockButton.lockName.setText(R.string.garage_door)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(javaClass.simpleName, "onViewCreated")
        binding!!.lockButton.locked.setOnClickListener {
            runOrSentry {
                myqClient.setGarageDoorState(MyQClient.GarageDoorState.OPEN)
                debounceFetchLockState()
                updateButton(MyQClient.GarageDoorState.OPENING)
            }
        }
        binding!!.lockButton.unlocked.setOnClickListener {
            runOrSentry {
                myqClient.setGarageDoorState(MyQClient.GarageDoorState.CLOSED)
                debounceFetchLockState()
                updateButton(MyQClient.GarageDoorState.CLOSING)
            }
        }

        updateButton(null)
    }


    fun fetchLockState() {
        runOrSentry {
            val garageDoorState = myqClient.fetchGarageDoorState().get()
            Log.i(javaClass.simpleName, "MyQ Garage state $garageDoorState")
            updateButton(garageDoorState)
        }
    }

    fun debounceFetchLockState() {
        schedule?.cancel(false)
        schedule = updater.scheduleWithFixedDelay(::fetchLockState, 5, 5, TimeUnit.SECONDS)
    }

    fun updateButton(garageDoorState: MyQClient.GarageDoorState?) {
        CoroutineScope(Dispatchers.Main).launch {
            when (garageDoorState) {
                MyQClient.GarageDoorState.OPEN -> {
                    binding!!.lockButton.loading.visibility = View.GONE
                    binding!!.lockButton.unlocked.visibility = View.VISIBLE
                    binding!!.lockButton.locked.visibility = View.GONE
                    binding!!.lockButton.statusText.visibility = View.GONE
                }
                MyQClient.GarageDoorState.CLOSED -> {
                    binding!!.lockButton.loading.visibility = View.GONE
                    binding!!.lockButton.unlocked.visibility = View.GONE
                    binding!!.lockButton.locked.visibility = View.VISIBLE
                    binding!!.lockButton.statusText.visibility = View.GONE
                }
                MyQClient.GarageDoorState.OPENING -> {
                    binding!!.lockButton.loading.visibility = View.VISIBLE
                    binding!!.lockButton.unlocked.visibility = View.GONE
                    binding!!.lockButton.locked.visibility = View.GONE
                    binding!!.lockButton.statusText.visibility = View.VISIBLE
                    binding!!.lockButton.statusText.text = "Opening"
                }
                MyQClient.GarageDoorState.CLOSING -> {
                    binding!!.lockButton.loading.visibility = View.VISIBLE
                    binding!!.lockButton.unlocked.visibility = View.GONE
                    binding!!.lockButton.locked.visibility = View.GONE
                    binding!!.lockButton.statusText.visibility = View.VISIBLE
                    binding!!.lockButton.statusText.text = "Closing"
                }
                else  -> {
                    binding!!.lockButton.loading.visibility = View.VISIBLE
                    binding!!.lockButton.unlocked.visibility = View.GONE
                    binding!!.lockButton.locked.visibility = View.GONE
                    binding!!.lockButton.statusText.visibility = View.VISIBLE
                    binding!!.lockButton.statusText.text = "Loading"
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        schedule = updater.scheduleWithFixedDelay(::fetchLockState, 0, 5, TimeUnit.SECONDS);
    }

    override fun onPause() {
        super.onPause()
        schedule?.cancel(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        updater.shutdown()
        super.onDestroy()
    }
}