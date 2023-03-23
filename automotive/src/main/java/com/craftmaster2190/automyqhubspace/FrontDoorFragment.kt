package com.craftmaster2190.automyqhubspace

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.craftmaster2190.automyqhubspace.databinding.FragmentFrontDoorBinding
import com.craftmaster2190.automyqhubspace.ui.login.AppCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class FrontDoorFragment : Fragment() {
    private val hubSpaceClient by lazy {
        HubSpaceClient.instance.also { hubSpaceClient_ ->
            AppCredentials.fromContext(this.requireContext())?.also {
                hubSpaceClient_.username = it.hubspaceUsername
                hubSpaceClient_.password = it.hubspacePassword
            }
        }
    }
    private val updater = Executors.newSingleThreadScheduledExecutor()
    private var schedule: ScheduledFuture<*>? = null

    private var binding: FragmentFrontDoorBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFrontDoorBinding.inflate(inflater, container, false)
        binding!!.lockButton.lockName.setText(R.string.front_door)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(javaClass.simpleName, "onViewCreated")
        binding!!.lockButton.locked.setOnClickListener {
            runOrSentry {
                hubSpaceClient.setDeviceState(HubSpaceClient.FrontDoorState.UNLOCKED)
                debounceFetchLockState()
                updateButton(HubSpaceClient.FrontDoorState.UNLOCKING)
            }
        }
        binding!!.lockButton.unlocked.setOnClickListener {
            runOrSentry {
                hubSpaceClient.setDeviceState(HubSpaceClient.FrontDoorState.LOCKED)
                debounceFetchLockState()
                updateButton(HubSpaceClient.FrontDoorState.LOCKING)
            }
        }

        updateButton(null)
    }

    fun fetchLockState() {
        runOrSentry {
            val doorlockState = hubSpaceClient.fetchDeviceState()
            Log.i(javaClass.simpleName, "HubSpace Frontdoor state $doorlockState")
            updateButton(doorlockState)
        }
    }

    fun debounceFetchLockState() {
        schedule?.cancel(false)
        schedule = updater.scheduleWithFixedDelay(::fetchLockState, 5, 5, TimeUnit.SECONDS)
    }

    fun updateButton(frontDoorState: HubSpaceClient.FrontDoorState?) {
        CoroutineScope(Dispatchers.Main).launch {
            when (frontDoorState) {
                HubSpaceClient.FrontDoorState.UNLOCKED -> {
                    binding!!.lockButton.loading.visibility = View.GONE
                    binding!!.lockButton.unlocked.visibility = View.VISIBLE
                    binding!!.lockButton.locked.visibility = View.GONE
                    binding!!.lockButton.statusText.visibility = View.GONE
                }
                HubSpaceClient.FrontDoorState.LOCKED -> {
                    binding!!.lockButton.loading.visibility = View.GONE
                    binding!!.lockButton.unlocked.visibility = View.GONE
                    binding!!.lockButton.locked.visibility = View.VISIBLE
                    binding!!.lockButton.statusText.visibility = View.GONE
                }
                HubSpaceClient.FrontDoorState.LOCKING -> {
                    binding!!.lockButton.loading.visibility = View.VISIBLE
                    binding!!.lockButton.unlocked.visibility = View.GONE
                    binding!!.lockButton.locked.visibility = View.GONE
                    binding!!.lockButton.statusText.visibility = View.VISIBLE
                    binding!!.lockButton.statusText.text = "Locking"
                }
                HubSpaceClient.FrontDoorState.UNLOCKING -> {
                    binding!!.lockButton.loading.visibility = View.VISIBLE
                    binding!!.lockButton.unlocked.visibility = View.GONE
                    binding!!.lockButton.locked.visibility = View.GONE
                    binding!!.lockButton.statusText.visibility = View.VISIBLE
                    binding!!.lockButton.statusText.text = "Unlocking"
                }
                else -> {
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