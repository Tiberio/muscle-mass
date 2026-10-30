import importlib.util
import tempfile
from pathlib import Path
import unittest

FILE = Path(__file__).with_name('pesquisar_86_videos_youtube.py')
spec = importlib.util.spec_from_file_location('ytsearch', FILE)
s = importlib.util.module_from_spec(spec)
spec.loader.exec_module(s)

class VideoResearchTests(unittest.TestCase):
    def test_duration(self):
        self.assertEqual(s.duration('PT12S'), 12)
        self.assertEqual(s.duration('PT1M25S'), 85)
        self.assertIsNone(s.duration('not-an-iso'))

    def test_title_prioritization(self):
        self.assertGreater(s.rank_title('Machine Chest Press', 'Machine Chest Press - proper technique'),
                           s.rank_title('Machine Chest Press', 'Calf Press Machine'))

    def test_research_and_report_with_stub(self):
        original=s.api_get
        def fake(url, params, key):
            if url==s.SEARCH_URL:
                return {'items':[{'id':{'videoId':'AbcDeF_0123'},'snippet':{
                    'title':'Bench Dips Demonstration','channelTitle':'Example Channel'}}]}
            return {'items':[{'id':'AbcDeF_0123','snippet':{'title':'Bench Dips Demonstration','channelTitle':'Example Channel'},
                'contentDetails':{'duration':'PT18S'},'status':{'embeddable':True,'privacyStatus':'public'}}]}
        s.api_get=fake
        try:
            r=s.run([], [{'exercise_id':'benchedips','nome_pt':'Mergulho no banco','nome_en':'Bench Dips'}],'fake-key')
            self.assertEqual(r['total_pendentes_pesquisados'],1)
            self.assertEqual(r['pesquisas'][0]['opcoes'][0]['duracao_segundos'],18)
            with tempfile.TemporaryDirectory() as d:
                s.save_report(r,Path(d));self.assertTrue((Path(d)/'RESULTADO_PESQUISA_130.json').exists())
        finally:
            s.api_get=original

if __name__=='__main__':unittest.main()
