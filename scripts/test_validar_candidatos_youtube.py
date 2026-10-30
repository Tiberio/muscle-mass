import unittest
from validar_candidatos_youtube import duration_seconds, evaluate


def sample(duration='PT38S', embeddable=True, region=None):
    return {
        'snippet': {'title': 'Dumbbell Bench Press Exercise Demo', 'channelTitle': 'Channel', 'liveBroadcastContent': 'none'},
        'contentDetails': {'duration': duration, 'regionRestriction': region or {}},
        'status': {'embeddable': embeddable, 'privacyStatus': 'public'},
    }


class TestValidation(unittest.TestCase):
    item = {'exercise_id': 'dbbench', 'youtube_id': 'PziFsPH2EZI', 'matched_title': 'Dumbbell Bench Press'}

    def test_duration_iso(self):
        self.assertEqual(duration_seconds('PT1M05S'), 65)
        self.assertEqual(duration_seconds('PT40S'), 40)
        self.assertIsNone(duration_seconds('bad'))

    def test_short_video_requires_visual(self):
        r = evaluate(self.item, sample('PT38S'))
        self.assertEqual(r['status'], 'CURTO_APTO_A_REVISAO_VISUAL')
        self.assertTrue(r['revisao_visual_obrigatoria'])

    def test_long_video_requires_clip(self):
        r = evaluate(self.item, sample('PT41S'))
        self.assertEqual(r['status'], 'PRECISA_RECORTE_E_REVISAO_VISUAL')

    def test_no_embed(self):
        self.assertEqual(evaluate(self.item, sample(embeddable=False))['status'], 'PROBLEMA_TECNICO')

    def test_region_block(self):
        self.assertEqual(evaluate(self.item, sample(region={'blocked': ['BR']}))['status'], 'PROBLEMA_TECNICO')

    def test_missing_video(self):
        self.assertEqual(evaluate(self.item, None)['status'], 'PROBLEMA_TECNICO')


if __name__ == '__main__':
    unittest.main()
