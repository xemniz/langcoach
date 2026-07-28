package com.xemniz.langcoach.ui.call

import com.xemniz.langcoach.llm.realtime.RealtimeResponseStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VoiceTurnStateMachineTest {
    @Test
    fun completedResponseDrainsBeforeMicrophoneReopens() {
        val responding = reduceVoiceTurn(
            VoiceTurnState.Listening,
            VoiceTurnEvent.ResponseStarted("resp_a"),
        ).state
        val transition = reduceVoiceTurn(
            responding,
            VoiceTurnEvent.ResponseFinished(
                responseId = "resp_a",
                status = RealtimeResponseStatus.Completed,
                reason = null,
            ),
        )

        assertEquals(VoiceTurnState.Draining("resp_a"), transition.state)
        assertEquals(
            listOf(VoiceTurnEffect.DrainPlayback("resp_a")),
            transition.effects,
        )
        assertFalse(transition.state.acceptsMicrophone)

        val drained = reduceVoiceTurn(
            transition.state,
            VoiceTurnEvent.PlaybackDrained("resp_a"),
        ).state
        assertEquals(VoiceTurnState.Listening, drained)
        assertTrue(drained.acceptsMicrophone)
    }

    @Test
    fun staleDrainCannotFinishANewerResponse() {
        val drainingA = VoiceTurnState.Draining("resp_a")
        val respondingB = reduceVoiceTurn(
            drainingA,
            VoiceTurnEvent.ResponseStarted("resp_b"),
        ).state

        val afterStaleDrain = reduceVoiceTurn(
            respondingB,
            VoiceTurnEvent.PlaybackDrained("resp_a"),
        ).state

        assertEquals(VoiceTurnState.Responding("resp_b"), afterStaleDrain)
        assertFalse(afterStaleDrain.acceptsMicrophone)
    }

    @Test
    fun staleDoneCannotMoveANewerResponseToDraining() {
        val respondingB = VoiceTurnState.Responding("resp_b")

        val state = reduceVoiceTurn(
            respondingB,
            VoiceTurnEvent.ResponseFinished(
                responseId = "resp_a",
                status = RealtimeResponseStatus.Completed,
                reason = null,
            ),
        ).state

        assertEquals(respondingB, state)
    }

    @Test
    fun interruptedTransportDrainsAudibleAudioBeforeListening() {
        val transition = reduceVoiceTurn(
            VoiceTurnState.Responding("resp_a"),
            VoiceTurnEvent.TransportInterrupted,
        )

        assertEquals(VoiceTurnState.Draining("resp_a"), transition.state)
        assertEquals(
            listOf(VoiceTurnEffect.DrainPlayback("resp_a")),
            transition.effects,
        )
    }

    @Test
    fun incompleteResponseBecomesAnExplicitFailure() {
        val state = reduceVoiceTurn(
            VoiceTurnState.Responding("resp_a"),
            VoiceTurnEvent.ResponseFinished(
                responseId = "resp_a",
                status = RealtimeResponseStatus.Incomplete,
                reason = "max_output_tokens",
            ),
        ).state

        val failure = assertIs<VoiceTurnState.Failed>(state)
        assertEquals("max_output_tokens", failure.message)
    }

    @Test
    fun coordinatorOnlySendsMicrophoneAudioWhileConnectedAndListening() {
        val listening = RealtimeCallSnapshot(
            connection = RealtimeConnectionState.Connected,
            turn = VoiceTurnState.Listening,
        )

        assertTrue(listening.canSendMicrophone)
        assertFalse(listening.copy(isMuted = true).canSendMicrophone)
        assertFalse(
            listening.copy(turn = VoiceTurnState.Responding("resp_a")).canSendMicrophone,
        )
        assertFalse(
            listening.copy(connection = RealtimeConnectionState.Reconnecting)
                .canSendMicrophone,
        )
    }
}
