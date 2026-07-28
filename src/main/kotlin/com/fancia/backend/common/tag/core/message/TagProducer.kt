package com.fancia.backend.common.tag.core.message

import com.fancia.backend.shared.common.tag.core.message.TagDeletedEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class TagProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun publishTagDeleted(event: TagDeletedEvent) {
        kafkaTemplate.send("tags", event.id.toString(), event)
            .whenComplete { _, _ -> }
    }
}