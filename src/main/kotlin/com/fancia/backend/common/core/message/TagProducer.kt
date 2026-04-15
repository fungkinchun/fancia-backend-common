package com.fancia.backend.common.core.message

import com.fancia.backend.shared.common.core.message.TagDeletedEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class TagProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun publishTagDeleted(event: TagDeletedEvent) {
        kafkaTemplate.send("tags", event.name, event)
            .whenComplete { result, ex -> }
    }
}