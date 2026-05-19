from app.meeting_summary import MeetingSummaryChain


def test_manual_parse_output_with_prefixed_json_and_alias_key():
    chain = MeetingSummaryChain()
    raw = (
        'Valid JSON output:\n'
        '{\\"summary\\":\\"요약\\",\\"decisions\\":[\\"결정\\"],'
        '\\"action_items\\":[{\\"task\\":\\"할일\\",\\"owner\\":\\"홍길동\\",\\"due_date\\":\\"미정\\"}],'
        '\\"risks\\":[],\\"open_question\\":[]}'
    )

    parsed = chain._manual_parse_output(raw)
    assert parsed.summary == "요약"
    assert parsed.open_questions == []
    assert parsed.action_items[0].owner == "홍길동"


def test_split_transcript_respects_max_chunks():
    chain = MeetingSummaryChain(chunk_size=20, chunk_overlap=0, max_chunks=3)
    transcript = "abcdefghijklmnopqrstuvwxyz" * 10

    chunks = chain._split_transcript(transcript)
    reconstructed = "".join(chunks).replace("\n", "")

    assert len(chunks) == 3
    assert reconstructed == transcript
