#!/usr/bin/env python3
"""Auditoria de metadados dos candidatos do Meu Treino via YouTube Data API v3.
Nao baixa videos nem altera automaticamente o catalogo do app.
"""
import argparse
import json
import os
from pathlib import Path
import re
import sys
import urllib.parse
import urllib.request
from urllib.error import HTTPError, URLError
from datetime import datetime, timezone
from difflib import SequenceMatcher

API_URL = 'https://www.googleapis.com/youtube/v3/videos'
ID_RE = re.compile(r'^[A-Za-z0-9_-]{11}$')
ISO_RE = re.compile(r'^P(?:(?P<days>\d+)D)?(?:T(?:(?P<hours>\d+)H)?(?:(?P<minutes>\d+)M)?(?:(?P<seconds>\d+)S)?)?$')


def duration_seconds(iso):
    """Converte duracao ISO 8601 do YouTube para segundos sem bibliotecas externas."""
    match = ISO_RE.fullmatch(iso or '')
    if not match:
        return None
    values = {k: int(v or 0) for k, v in match.groupdict().items()}
    return values['days'] * 86400 + values['hours'] * 3600 + values['minutes'] * 60 + values['seconds']


def fetch_videos(video_ids, api_key):
    found = {}
    ids = sorted(set(video_ids))
    for start in range(0, len(ids), 50):
        params = urllib.parse.urlencode({
            'part': 'snippet,contentDetails,status',
            'id': ','.join(ids[start:start + 50]),
            'key': api_key,
        })
        req = urllib.request.Request(API_URL + '?' + params, headers={'Accept': 'application/json'})
        try:
            with urllib.request.urlopen(req, timeout=25) as response:
                payload = json.load(response)
        except HTTPError as exc:
            # Nunca incluir URL contendo chave no log.
            raise RuntimeError(f'YouTube Data API retornou HTTP {exc.code}. Verifique chave, API habilitada e cota.') from None
        except (URLError, TimeoutError):
            raise RuntimeError('Falha de rede ao consultar YouTube Data API.') from None
        found.update({video['id']: video for video in payload.get('items', [])})
    return found


def evaluate(candidate, video):
    video_id = candidate.get('youtube_id', '')
    row = {
        'exercise_id': candidate.get('exercise_id', ''),
        'youtube_id': video_id,
        'link': f'https://www.youtube.com/watch?v={video_id}',
        'titulo_candidato': candidate.get('matched_title', ''),
        'titulo_youtube': '', 'canal': '',
        'duracao_s': None, 'incorporavel': False,
        'disponivel_no_br': None,
        'status': '', 'motivos': [],
        'revisao_visual_obrigatoria': True,
    }
    if not ID_RE.fullmatch(video_id):
        row['status'] = 'PROBLEMA_TECNICO'
        row['motivos'].append('ID de video invalido')
        return row
    if not video:
        row['status'] = 'PROBLEMA_TECNICO'
        row['motivos'].append('Video nao retornado pela API (removido, privado ou indisponivel)')
        return row
    snippet = video.get('snippet', {})
    details = video.get('contentDetails', {})
    status = video.get('status', {})
    row['titulo_youtube'] = snippet.get('title', '')
    row['canal'] = snippet.get('channelTitle', '')
    row['duracao_s'] = duration_seconds(details.get('duration', ''))
    row['incorporavel'] = status.get('embeddable') is True
    restriction = details.get('regionRestriction', {})
    allowed = restriction.get('allowed')
    blocked = restriction.get('blocked', [])
    row['disponivel_no_br'] = ('BR' in allowed if allowed is not None else 'BR' not in blocked)
    if status.get('privacyStatus', 'public') not in ('public', 'unlisted'):
        row['motivos'].append('Video nao publico/unlisted')
    if not row['incorporavel']:
        row['motivos'].append('Autor nao permite incorporacao')
    if not row['disponivel_no_br']:
        row['motivos'].append('Restricao regional para BR')
    if snippet.get('liveBroadcastContent', 'none') != 'none':
        row['motivos'].append('Conteudo ao vivo/programado; inadequado a demonstracao curta')
    if row['duracao_s'] is None or row['duracao_s'] == 0:
        row['motivos'].append('Duracao ausente ou invalida')
    if row['motivos']:
        row['status'] = 'PROBLEMA_TECNICO'
        return row
    if row['duracao_s'] <= 40:
        row['status'] = 'CURTO_APTO_A_REVISAO_VISUAL'
    else:
        row['status'] = 'PRECISA_RECORTE_E_REVISAO_VISUAL'
    # Nao interpretar baixo grau de similaridade como erro: candidatos podem estar em idiomas diferentes.
    a = row['titulo_candidato'].casefold().strip()
    b = row['titulo_youtube'].casefold().strip()
    if a and b and SequenceMatcher(None, a, b).ratio() < 0.4:
        row['motivos'].append('Titulos diferentes; conferir variante, equipamento e execucao')
    return row


def escape_table(value):
    return str(value if value is not None else '—').replace('|', '\\|').replace('\n', ' ').replace('\r', ' ')


def produce_report(rows, output_dir):
    output_dir.mkdir(parents=True, exist_ok=True)
    with (output_dir / 'resultado_validacao.json').open('w', encoding='utf-8') as f:
        json.dump({'created_at_utc': datetime.now(timezone.utc).isoformat(),
                   'notes': 'Metadados somente. Nenhum video tem tecnica aprovada automaticamente.',
                   'results': rows}, f, ensure_ascii=False, indent=2)
    counts = {name: sum(r['status'] == name for r in rows) for name in
              ('CURTO_APTO_A_REVISAO_VISUAL','PRECISA_RECORTE_E_REVISAO_VISUAL','PROBLEMA_TECNICO')}
    doc = [
        '# Auditoria automatica de videos candidatos — Meu Treino', '',
        f'Exercicios com candidato avaliados: **{len(rows)}**.',
        f'- Curtos (ate 40 s) aptos a revisao visual: **{counts["CURTO_APTO_A_REVISAO_VISUAL"]}**',
        f'- Longos que precisam de selecao de trecho: **{counts["PRECISA_RECORTE_E_REVISAO_VISUAL"]}**',
        f'- Indisponiveis ou com problemas tecnicos: **{counts["PROBLEMA_TECNICO"]}**', '',
        '**IMPORTANTE:** Nenhum destes resultados prova que o movimento esta tecnicamente correto,',
        'nem que o video corresponde a variante e aos equipamentos do exercicio.',
        'Os videos longos precisam de visualizacao para definir o corte completo com 2–4 s de margem.', '',
        '| Exercicio (ID) | Link | Video do YouTube | Duracao | Permite player | Situacao | Observacoes |',
        '|---|---|---|---:|---|---|---|'
    ]
    for row in rows:
        fields = [
            row['exercise_id'], f'[Abrir]({row["link"]})', row['titulo_youtube'] or 'Nao encontrado',
            (f'{row["duracao_s"]} s' if row['duracao_s'] is not None else '—'),
            ('Sim' if row['incorporavel'] else 'Nao'), row['status'],
            '; '.join(row['motivos']) or 'Revisar tecnica e variante',
        ]
        doc.append('| ' + ' | '.join(escape_table(field) for field in fields) + ' |')
    doc += ['', '## Procedimento apos auditoria', '',
            '1. Conferir visualmente os exercicios (inclusive curtos).',
            '2. Para videos longos, escolher trecho com movimento completo e 2–4 segundos de margem.',
            '3. Somente entao promover o candidato de `candidates` para `videos` no catalogo.',
            '4. Validar novamente o JSON e compilar o APK.', '']
    (output_dir / 'RELATORIO_VALIDACAO.md').write_text('\n'.join(doc), encoding='utf-8')
    return counts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--catalog', default='app/src/main/assets/exercise_videos.json')
    parser.add_argument('--output', default='reports/videos_yt')
    args = parser.parse_args()
    api_key = os.getenv('YOUTUBE_API_KEY')
    if not api_key:
        print('Erro: configure YOUTUBE_API_KEY como secret do repositorio.', file=sys.stderr)
        return 2
    try:
        data = json.loads(Path(args.catalog).read_text(encoding='utf-8'))
        candidates = data['candidates']
        if len(candidates) != 60:
            print(f'Aviso: encontrados {len(candidates)} candidatos; eram 60 na v0.4.5.', file=sys.stderr)
        all_ids = [c.get('youtube_id', '') for c in candidates]
        videos = fetch_videos([v for v in all_ids if ID_RE.fullmatch(v)], api_key)
        rows = [evaluate(item, videos.get(item.get('youtube_id'))) for item in candidates]
        counts = produce_report(rows, Path(args.output))
    except (KeyError, ValueError, OSError, RuntimeError) as exc:
        print(f'Erro de validacao: {exc}', file=sys.stderr)
        return 1
    print(f'Analisados: {len(rows)} | Curtos: {counts["CURTO_APTO_A_REVISAO_VISUAL"]} | '
          f'Longos: {counts["PRECISA_RECORTE_E_REVISAO_VISUAL"]} | '
          f'Problemas: {counts["PROBLEMA_TECNICO"]}')
    print('Relatorios salvos em:', args.output)
    return 0


if __name__ == '__main__':
    sys.exit(main())
