package br.com.meutreino.domain

/**
 * Exercícios novos da v0.4.0. Cada linha representa um movimento ou uma variante
 * biomecanicamente distinta: id | nome | padrão | músculo principal | secundários |
 * equipamentos obrigatórios | nível mínimo (0..2) | orientação específica.
 *
 * Estes nomes e instruções são dados locais do app. Não atribuem validação clínica
 * individual nem incluem links externos sem verificação.
 */
internal object ExerciseLibraryData {
    private val rows = """
wallpush|Flexão na parede|push|Peito|Braços,Ombros||0|Apoie as palmas na altura do peito e mantenha o corpo alinhado.
kneepush|Flexão com joelhos apoiados|push|Peito|Braços,Ombros||0|Mantenha quadril e ombros alinhados durante a flexão.
closepush|Flexão com mãos próximas|push|Peito|Braços,Ombros||1|Aproxime as mãos sem forçar os punhos e controle a descida.
widepush|Flexão com mãos afastadas|push|Peito|Braços,Ombros||1|Não abra exageradamente os cotovelos nem force a amplitude.
declinepush|Flexão com pés elevados|push|Peito|Braços,Ombros|Banco|1|Apoie os pés em superfície estável e sustente a região lombar.
tempopush|Flexão com descida lenta|push|Peito|Braços,Ombros||1|Desça contando três segundos e suba sem perder alinhamento.
pausepush|Flexão com pausa inferior|push|Peito|Braços,Ombros||1|Faça breve pausa próxima ao solo sem relaxar o tronco.
archerpush|Flexão arqueiro|push|Peito|Braços,Ombros||2|Distribua a carga gradualmente para um dos braços.
pseudoplanche|Flexão pseudo-planche|push|Peito|Ombros,Braços||2|Avance levemente os ombros além das mãos mantendo controle dos punhos.
explosivepush|Flexão explosiva|push|Peito|Braços,Ombros||2|Só acelere a subida quando dominar a flexão convencional.
spidermanpush|Flexão com joelho lateral|push|Peito|Braços,Abdômen||1|Aproxime o joelho do cotovelo sem rodar excessivamente o quadril.
hindupush|Flexão hindu|push|Ombros|Peito,Braços||2|Realize movimento controlado em arco, sem forçar a lombar.
pikepush|Flexão pike|verticalPush|Ombros|Braços,Peito||1|Mantenha o quadril elevado e a cabeça entre as mãos.
elevatedpike|Flexão pike com pés elevados|verticalPush|Ombros|Braços,Peito|Banco|2|Eleve os pés em suporte firme, com controle cervical.
wallhandstandpush|Flexão de ombros na parede (parcial)|verticalPush|Ombros|Braços||2|Use amplitude reduzida e apoio firme, sem sobrecarregar o pescoço.
benchedips|Mergulho no banco|push|Braços|Peito,Ombros|Banco|1|Não desça além da mobilidade confortável dos ombros.
paralleldips|Paralelas (mergulho)|push|Peito|Braços,Ombros|Paralelas|2|Mantenha o movimento controlado e a amplitude tolerável.
assistedparalleldips|Paralelas com pés apoiados|push|Braços|Peito,Ombros|Paralelas|1|Use apoio dos pés para modular a carga e manter estabilidade.
invertedrow|Remada australiana na barra baixa|pull|Costas|Braços|Barra fixa|1|Mantenha tronco rígido e puxe o peito em direção à barra.
scapularpull|Depressão escapular suspenso na barra|pull|Costas|Ombros,Braços|Barra fixa|1|Eleve o corpo levemente pelas escápulas, sem dobrar os cotovelos.
negativepull|Barra fixa excêntrica|pull|Costas|Braços|Barra fixa|1|Desça lentamente usando apoio seguro para alcançar a posição alta.
strictpull|Barra fixa pronada|pull|Costas|Braços|Barra fixa|2|Controle toda a amplitude e evite balanço do corpo.
chinup|Barra fixa supinada|pull|Costas|Braços|Barra fixa|2|Puxe sem chutar as pernas, mantendo pescoço neutro.
neutralpull|Barra fixa pegada neutra|pull|Costas|Braços|Barra fixa|2|Utilize barra adequada para pegada neutra sem torcer os punhos.
commando|Barra fixa com pegada alternada|pull|Costas|Braços|Barra fixa|2|Alterne o lado da cabeça em relação à barra com controle.
archerpull|Barra fixa arqueiro|pull|Costas|Braços|Barra fixa|2|Progrida apenas após dominar várias barras estritas.
bodyweightrowrings|Remada suspensa (TRX/argolas)|pull|Costas|Braços|Suspensão/TRX|1|Ajuste a inclinação do corpo para controlar a dificuldade.
ringfacepull|Face pull suspenso (TRX/argolas)|pull|Ombros|Costas,Braços|Suspensão/TRX|1|Puxe as mãos em direção ao rosto e mantenha as escápulas ativas.
proneyraise|Elevação Y deitado|isolation|Ombros|Costas||0|Eleve os braços em Y com baixa amplitude e sem forçar o pescoço.
pronewraise|Elevação W deitado|isolation|Ombros|Costas||0|Aproxime as escápulas com cotovelos flexionados sem erguer a lombar.
wallslide|Deslizamento de braços na parede|isolation|Ombros|Costas||0|Suba os braços deslizando na parede sem arquear a lombar.
scapularpush|Flexão escapular|push|Ombros|Costas||0|Mantenha cotovelos estendidos e mova somente as escápulas.
shouldertap|Toques no ombro em prancha|core|Abdômen|Ombros||0|Toque alternadamente os ombros sem rodar excessivamente a pelve.
regressedsquat|Sentar e levantar da cadeira|squat|Pernas|Glúteos|Banco|0|Sente-se suavemente e levante sem impulsionar com os braços.
temposquat|Agachamento livre com descida lenta|squat|Pernas|Glúteos||0|Desça em três segundos mantendo os calcanhares apoiados.
pausesquat|Agachamento com pausa inferior|squat|Pernas|Glúteos||1|Faça pausa curta na amplitude confortável, mantendo o controle.
sumosquat|Agachamento sumô livre|squat|Pernas|Glúteos||0|Abra a base sem exceder a mobilidade do quadril.
wallseat|Agachamento isométrico na parede|squat|Pernas|Glúteos||0|Mantenha joelhos alinhados aos pés e não prenda a respiração.
jumpsquat|Agachamento com salto|squat|Pernas|Glúteos||2|Aterrisse suavemente e suspenda se houver dor articular.
cyclistsquat|Agachamento com calcanhares elevados|squat|Pernas|Glúteos||1|Use elevação baixa e firme sob os calcanhares.
pistolsupport|Agachamento unilateral com apoio|squat|Pernas|Glúteos|Banco|1|Use apoio estável para equilibrar e regular a amplitude.
pistolsquat|Agachamento pistola|squat|Pernas|Glúteos||2|Execute apenas com mobilidade e força suficientes.
shrimp|Agachamento shrimp|squat|Pernas|Glúteos||2|Mantenha equilíbrio e avance gradualmente na amplitude.
reverselunge|Afundo reverso|lunge|Pernas|Glúteos||0|Dê passo para trás e retorne mantendo o tronco estável.
forwardlunge|Afundo à frente|lunge|Pernas|Glúteos||0|Pise de forma controlada sem colapsar o joelho para dentro.
latlunge|Afundo lateral|lunge|Pernas|Glúteos||1|Transfira o peso para uma perna sem perder estabilidade.
cossack|Agachamento cossaco|lunge|Pernas|Glúteos||2|Faça deslocamento lateral apenas na amplitude confortável.
splitbw|Agachamento dividido sem carga|lunge|Pernas|Glúteos||0|Mantenha um pé à frente do outro e flexione ambos os joelhos.
bulgarianbw|Agachamento búlgaro sem carga|lunge|Pernas|Glúteos|Banco|1|Apoie o pé traseiro no banco e desça sem perder o equilíbrio.
stepupbw|Subida no banco (step-up)|lunge|Pernas|Glúteos|Banco|1|Use plataforma baixa e firme; impulsione com a perna da frente.
curtsylunge|Afundo cruzado|lunge|Glúteos|Pernas||1|Cruze a perna para trás sem forçar a rotação do joelho.
jumplunge|Afundo com salto alternado|lunge|Pernas|Glúteos||2|Aterrisse suavemente, dominando antes a versão sem salto.
glutebridgeone|Ponte de glúteos unilateral|hinge|Glúteos|Pernas||1|Eleve o quadril sem deixar a pelve girar.
glutebridgepause|Ponte de glúteos isométrica|hinge|Glúteos|Pernas||0|Sustente o alto do movimento mantendo abdômen ativo.
hipthrustbw|Elevação pélvica apoiada no banco|hinge|Glúteos|Pernas|Banco|0|Apoie a parte superior das costas e evite hiperextensão lombar.
hamwalkout|Caminhada de calcanhares na ponte|hinge|Pernas|Glúteos||1|Afaste os pés aos poucos mantendo quadril elevado.
singlelegdeadliftbw|Inclinação de quadril unilateral sem carga|hinge|Pernas|Glúteos||1|Incline o tronco com controle e mantenha a pelve alinhada.
goodmorningbw|Bom dia sem carga|hinge|Pernas|Glúteos,Costas||0|Flexione o quadril preservando alinhamento da coluna.
superman|Extensão lombar no solo|hinge|Costas|Glúteos||0|Eleve braços e pernas pouco, sem forçar a lombar.
calfstand|Elevação de panturrilhas bilateral|calf|Pernas|||0|Eleve os calcanhares sem rodar os tornozelos.
calfone|Elevação de panturrilha unilateral|calf|Pernas|||1|Use apoio para equilíbrio e suba com controle.
calfstep|Elevação de panturrilhas no degrau|calf|Pernas||Step|1|Use degrau firme e amplitude sem dor no tendão.
tibialiswall|Dorsiflexão dos pés apoiado na parede|calf|Pernas|||0|Mova os dedos em direção às canelas sem perder apoio dos calcanhares.
sideplank|Prancha lateral|core|Abdômen|Ombros||0|Mantenha quadril alinhado e pescoço neutro.
sideplankleg|Prancha lateral com abdução de perna|core|Abdômen|Glúteos,Ombros||2|Eleve a perna sem cair o quadril.
reverseplank|Prancha invertida|core|Abdômen|Glúteos,Ombros||1|Eleve o quadril sem comprimir a região cervical.
hollowhold|Hollow body hold|core|Abdômen|||1|Mantenha a lombar suavemente apoiada e reduza a alavanca se necessário.
hollowrock|Hollow body rock|core|Abdômen|||2|Balance lentamente sem perder a posição abdominal.
reversecrunch|Abdominal reverso|core|Abdômen|||0|Eleve a pelve suavemente sem impulsionar as pernas.
crunch|Abdominal curto|core|Abdômen|||0|Flexione o tronco pouco e evite puxar o pescoço.
bicyclecrunch|Abdominal bicicleta|core|Abdômen|||1|Gire o tronco com controle sem puxar a cabeça.
legraise|Elevação de pernas deitado|core|Abdômen|||1|Reduza a amplitude se a lombar se afastar do solo.
kneetuck|Encolhimento de joelhos sentado|core|Abdômen|||0|Aproxime os joelhos do tronco controlando o equilíbrio.
bearhold|Posição de urso isométrica|core|Abdômen|Ombros,Pernas||0|Tire levemente os joelhos do chão sem arredondar a lombar.
bearcrawl|Deslocamento em quatro apoios (bear crawl)|core|Abdômen|Ombros,Pernas||1|Desloque mãos e pés lentamente sem balançar a pelve.
mountainclimber|Escalador (mountain climber)|core|Abdômen|Ombros,Pernas||1|Traga os joelhos ao peito preservando a prancha.
deadbugextended|Dead bug com pernas estendidas|core|Abdômen|||1|Estenda os membros apenas até manter controle da lombar.
birddogelbow|Bird dog com cotovelo ao joelho|core|Abdômen|Costas||1|Aproxime cotovelo e joelho sem rodar o tronco.
birdhold|Bird dog isométrico|core|Abdômen|Costas||0|Sustente membros opostos evitando inclinação lateral.
hangingknee|Elevação de joelhos suspenso|core|Abdômen|Braços|Barra fixa|1|Evite balanço e suba os joelhos com contração abdominal.
hangingleg|Elevação de pernas suspenso|core|Abdômen|Braços|Barra fixa|2|Progrida após dominar o exercício com joelhos flexionados.
lsitparallels|L-sit nas paralelas|core|Abdômen|Braços|Paralelas|2|Sustente os ombros deprimidos e progrida em tempo curto.
dbfloorpress|Supino no chão com halteres|push|Peito|Braços,Ombros|Halteres|0|Apoie os braços no solo e evite bater os cotovelos.
dbinclinepress|Supino inclinado com halteres|push|Peito|Braços,Ombros|Halteres,Banco|1|Ajuste o banco numa inclinação moderada e controle a carga.
dbdeclinepress|Supino declinado com halteres|push|Peito|Braços,Ombros|Halteres,Banco|1|Use banco próprio e apoios estáveis antes de iniciar.
dbfly|Crucifixo com halteres|isolation|Peito|Ombros|Halteres,Banco|1|Mantenha leve flexão nos cotovelos e não force abertura profunda.
dbinclinefly|Crucifixo inclinado com halteres|isolation|Peito|Ombros|Halteres,Banco|1|Faça arco confortável com cargas leves e controladas.
dbsqueeze|Supino squeeze com halteres|push|Peito|Braços|Halteres,Banco|1|Pressione os halteres entre si durante a subida.
dbpullover|Pullover com halter no banco|pull|Costas|Peito,Braços|Halteres,Banco|1|Mova ombros com amplitude confortável e tronco estável.
dbbentrow|Remada curvada com halteres|pull|Costas|Braços|Halteres|0|Flexione quadril e mantenha as escápulas sob controle.
dbchestrow|Remada com peito apoiado|pull|Costas|Braços|Halteres,Banco|0|Apoie o tórax no banco inclinado para evitar balanço.
dbwideelbowrow|Remada com cotovelos abertos|pull|Costas|Ombros,Braços|Halteres|1|Puxe cotovelos lateralmente sem forçar os ombros.
dbreversefly|Crucifixo invertido com halteres|pull|Ombros|Costas|Halteres|0|Abra os braços suavemente com tronco estável.
dbarnold|Desenvolvimento Arnold|verticalPush|Ombros|Braços|Halteres|1|Gire os antebraços com movimento confortável.
dbneutralpress|Desenvolvimento com pegada neutra|verticalPush|Ombros|Braços|Halteres|0|Mantenha punhos neutros e evite compensar pela lombar.
dbfrontraise|Elevação frontal com halteres|isolation|Ombros|Braços|Halteres|0|Suba até altura confortável sem balanço.
dblaterallean|Elevação lateral unilateral inclinada|isolation|Ombros|Braços|Halteres|1|Apoie a mão livre e eleve sem impulso.
dbyraise|Elevação em Y com halteres|isolation|Ombros|Costas|Halteres|1|Use carga leve e movimento controlado na diagonal.
dbshrug|Encolhimento com halteres|pull|Costas|Ombros|Halteres|0|Eleve ombros verticalmente sem rodá-los.
dbhammer|Rosca martelo|isolation|Braços||Halteres|0|Mantenha as palmas voltadas uma para a outra.
dbconcentration|Rosca concentrada|isolation|Braços||Halteres,Banco|0|Apoie o braço na coxa e evite balanço.
dbinclinecurl|Rosca inclinada com halteres|isolation|Braços||Halteres,Banco|1|Deixe os braços estáveis e não eleve os ombros.
dbspidercurl|Rosca spider no banco|isolation|Braços||Halteres,Banco|1|Apoie o peito no banco inclinado e flexione os cotovelos.
dbzottman|Rosca Zottman|isolation|Braços||Halteres|1|Gire os punhos gradualmente na fase descendente.
dbtricepsoverhead|Extensão de tríceps acima da cabeça|isolation|Braços||Halteres|0|Mantenha cotovelos apontados para cima sem forçar ombros.
dbtricepskick|Coice de tríceps com halter|isolation|Braços||Halteres|0|Estabilize o tronco e estenda o cotovelo sem balançar.
dbskullcrusher|Tríceps testa com halteres|isolation|Braços||Halteres,Banco|1|Desça perto da cabeça com controle e carga leve.
dbclosepress|Supino fechado com halteres|push|Peito|Braços|Halteres,Banco|1|Mantenha cotovelos relativamente próximos ao corpo.
dbfrontsquat|Agachamento com halteres nos ombros|squat|Pernas|Glúteos|Halteres|1|Apoie cargas confortáveis perto dos ombros e agache.
dbsumosquat|Agachamento sumô com halter|squat|Pernas|Glúteos|Halteres|0|Segure o halter entre as pernas com base confortável.
dbsplitsquat|Agachamento dividido com halteres|lunge|Pernas|Glúteos|Halteres|0|Mantenha tronco neutro e avance em amplitude controlada.
dbbulgarian|Agachamento búlgaro com halteres|lunge|Pernas|Glúteos|Halteres,Banco|1|Apoie pé traseiro firmemente e controle a descida.
dbstepup|Step-up com halteres|lunge|Pernas|Glúteos|Halteres,Banco|1|Use plataforma baixa e firme, sem impulsionar com o pé traseiro.
dbsingleRDL|Levantamento romeno unilateral com halter|hinge|Pernas|Glúteos|Halteres|1|Incline o quadril sem abrir a pelve lateralmente.
dbsumodead|Levantamento terra sumô com halter|hinge|Pernas|Glúteos|Halteres|0|Mantenha halter próximo ao corpo, evitando arredondar a coluna.
dbhipthrust|Elevação pélvica com halter|hinge|Glúteos|Pernas|Halteres,Banco|1|Posicione a carga confortavelmente sobre a pelve.
dbcalf|Elevação de panturrilhas com halteres|calf|Pernas||Halteres|0|Suba e desça com tornozelos alinhados.
dbfarmer|Caminhada do fazendeiro com halteres|carry|Abdômen|Braços,Costas|Halteres|0|Caminhe com tronco alto sem inclinar para os lados.
dbsuitcase|Caminhada com halter unilateral|carry|Abdômen|Braços,Costas|Halteres|1|Não incline o tronco para compensar a carga unilateral.
bbbench|Supino reto com barra|push|Peito|Braços,Ombros|Barra e anilhas,Banco|1|Mantenha escápulas apoiadas e use suportes de segurança.
bbincline|Supino inclinado com barra|push|Peito|Braços,Ombros|Barra e anilhas,Banco|1|Ajuste inclinação e use apoio para a barra.
bbclosebench|Supino fechado com barra|push|Peito|Braços,Ombros|Barra e anilhas,Banco|1|Use pegada confortável e não aproxime excessivamente as mãos.
bbrow|Remada curvada com barra|pull|Costas|Braços|Barra e anilhas|1|Mantenha coluna neutra e puxe a barra ao tronco.
bbpendlay|Remada Pendlay|pull|Costas|Braços|Barra e anilhas|2|Inicie cada repetição do solo sem flexionar a lombar.
bbunderrow|Remada supinada com barra|pull|Costas|Braços|Barra e anilhas|1|Controle a pegada supinada e estabilize o tronco.
bboverhead|Desenvolvimento militar com barra|verticalPush|Ombros|Braços|Barra e anilhas|1|Contraia abdômen e não hiperestenda a lombar.
bbpushpress|Push press com barra|verticalPush|Ombros|Pernas,Braços|Barra e anilhas|2|Use impulso curto de pernas e receba a barra com controle.
bbfrontsquat|Agachamento frontal com barra|squat|Pernas|Glúteos|Barra e anilhas|2|Mantenha os cotovelos altos e trabalhe com suportes seguros.
bbbacksquat|Agachamento com barra nas costas|squat|Pernas|Glúteos|Barra e anilhas|1|Use rack e barras de segurança ajustadas à sua altura.
bbboxsquat|Agachamento com barra até o banco|squat|Pernas|Glúteos|Barra e anilhas,Banco|1|Toque o banco de forma suave sem relaxar a lombar.
bbzercher|Agachamento Zercher|squat|Pernas|Glúteos,Braços|Barra e anilhas|2|Mantenha barra apoiada no antebraço com carga conservadora.
bbdeadlift|Levantamento terra convencional|hinge|Pernas|Glúteos,Costas|Barra e anilhas|1|Use a barra próxima às pernas e evite arredondar a lombar.
bbrdl|Levantamento romeno com barra|hinge|Pernas|Glúteos|Barra e anilhas|1|Desça com quadril para trás e barra próxima ao corpo.
bbsumodead|Levantamento terra sumô com barra|hinge|Pernas|Glúteos|Barra e anilhas|1|Ajuste a abertura confortável e mantenha joelhos na direção dos pés.
bbgoodmorning|Bom dia com barra|hinge|Pernas|Glúteos,Costas|Barra e anilhas|2|Mantenha carga reduzida e execute dobradiça do quadril.
bbhipthrust|Elevação pélvica com barra|hinge|Glúteos|Pernas|Barra e anilhas,Banco|1|Use proteção para a pelve e evite arquear a lombar.
bblunge|Afundo com barra|lunge|Pernas|Glúteos|Barra e anilhas|1|Controle equilíbrio com carga moderada.
bbcalf|Elevação de panturrilhas com barra|calf|Pernas||Barra e anilhas|1|Use suporte estável e movimento de tornozelo controlado.
bbcurl|Rosca direta com barra|isolation|Braços||Barra e anilhas|0|Mantenha cotovelos perto do corpo e evite embalo.
bbreversecurl|Rosca inversa com barra|isolation|Braços||Barra e anilhas|1|Use pegada pronada e carga confortável para punhos.
bbpreachercurl|Rosca Scott com barra|isolation|Braços||Barra e anilhas,Banco Scott|1|Exige banco Scott; não estenda os cotovelos sob carga brusca.
bbskull|Tríceps testa com barra|isolation|Braços||Barra e anilhas,Banco|1|Desça lentamente perto da testa sem abrir excessivamente os cotovelos.
bbupright|Remada alta com barra (amplitude confortável)|pull|Ombros|Braços|Barra e anilhas|1|Não eleve a barra se houver desconforto nos ombros.
inclinemachine|Supino inclinado em máquina|push|Peito|Braços,Ombros|Máquinas|0|Ajuste o banco para que as alças fiquem na altura adequada.
pecdeck|Voador peitoral (pec deck)|isolation|Peito|Ombros|Máquinas|0|Evite exagerar na abertura dos ombros.
cablefly|Crucifixo no cabo|isolation|Peito|Ombros|Máquinas|1|Mantenha cotovelos semiflexionados e retorne devagar.
cablechest|Supino em pé nos cabos|push|Peito|Braços,Ombros|Máquinas|1|Mantenha o tronco firme e empurre de forma simétrica.
latpulldown|Puxada frontal na polia|pull|Costas|Braços|Máquinas|0|Puxe em direção à parte superior do peito sem inclinar demais o tronco.
neutralpulldown|Puxada neutra na polia|pull|Costas|Braços|Máquinas|0|Mantenha ombros baixos e não puxe atrás da nuca.
closepulldown|Puxada fechada na polia|pull|Costas|Braços|Máquinas|0|Conduza os cotovelos para baixo sem puxar com balanço.
highrowmachine|Remada alta articulada|pull|Costas|Braços|Máquinas|0|Ajuste o assento para uma trajetória confortável.
chestrowmachine|Remada com apoio peitoral na máquina|pull|Costas|Braços|Máquinas|0|Apoie o tórax e evite estender a lombar.
machinepullover|Pullover na máquina|pull|Costas|Braços|Máquinas|1|Ajuste posição inicial sem forçar extensão do ombro.
assistedpullmachine|Barra fixa na máquina assistida|pull|Costas|Braços|Máquinas|0|Ajuste contrapeso para execução lenta e completa.
assisteddipmachine|Paralelas na máquina assistida|push|Braços|Peito,Ombros|Máquinas|0|Use assistência suficiente para controlar a amplitude.
shouldermachine|Desenvolvimento de ombros na máquina|verticalPush|Ombros|Braços|Máquinas|0|Ajuste o banco e não force a amplitude superior.
lateralmachine|Elevação lateral na máquina|isolation|Ombros||Máquinas|0|Empurre pelos apoios sem elevar os ombros.
reversepecdeck|Voador invertido|pull|Ombros|Costas|Máquinas|0|Tracione com ombros confortáveis e sem embalo.
facepullcable|Face pull no cabo|pull|Ombros|Costas,Braços|Máquinas|0|Puxe a corda em direção ao rosto com cotovelos altos.
legextension|Cadeira extensora|isolation|Pernas||Máquinas|0|Ajuste o eixo da máquina ao joelho e evite trancos.
seatedlegcurl|Mesa flexora sentada|isolation|Pernas||Máquinas|0|Controle o retorno sem deixar a carga despencar.
lyinglegcurl|Mesa flexora deitada|isolation|Pernas||Máquinas|0|Evite levantar a pelve ao flexionar os joelhos.
hacksquat|Agachamento hack na máquina|squat|Pernas|Glúteos|Máquinas|1|Ajuste a base dos pés para alinhamento confortável.
smithsquat|Agachamento no Smith|squat|Pernas|Glúteos|Máquinas|0|Regule travas e posicionamento dos pés antes de iniciar.
smithsplit|Agachamento dividido no Smith|lunge|Pernas|Glúteos|Máquinas|1|Controle a posição dos pés em relação à trajetória fixa.
hipabduction|Cadeira abdutora|isolation|Glúteos||Máquinas|0|Afaste as coxas sem impulsionar o tronco.
hipadduction|Cadeira adutora|isolation|Pernas||Máquinas|0|Feche as pernas em amplitude confortável.
hipthrustmachine|Elevação pélvica na máquina|hinge|Glúteos|Pernas|Máquinas|0|Ajuste almofada e faça extensão de quadril sem hiperextender.
glutekickmachine|Coice de glúteos na máquina|hinge|Glúteos|Pernas|Máquinas|0|Movimente a perna sem compensar pela lombar.
seatedcalf|Panturrilha sentada na máquina|calf|Pernas||Máquinas|0|Faça flexão plantar controlada sem saltos.
standingcalfmachine|Panturrilha em pé na máquina|calf|Pernas||Máquinas|0|Não use impulso dos joelhos para mover a carga.
cablecurl|Rosca bíceps na polia baixa|isolation|Braços||Máquinas|0|Mantenha cotovelos estáveis e retorne lentamente.
cablehammercurl|Rosca martelo no cabo com corda|isolation|Braços||Máquinas|0|Mantenha pegada neutra e tronco quieto.
preachermachine|Rosca Scott na máquina|isolation|Braços||Máquinas|0|Regule o banco para estabilizar os braços.
tricepsrope|Tríceps na polia com corda|isolation|Braços||Máquinas|0|Estenda cotovelos sem movimentar o tronco.
tricepsvbar|Tríceps na polia com barra V|isolation|Braços||Máquinas|0|Mantenha punhos alinhados sem estender demais.
tricepsoverheadcable|Tríceps acima da cabeça no cabo|isolation|Braços||Máquinas|1|Estabilize ombros antes de estender cotovelos.
cablekickback|Coice de tríceps na polia|isolation|Braços||Máquinas|1|Não balance os ombros ao estender os braços.
crunchmachine|Abdominal na máquina|core|Abdômen||Máquinas|0|Flexione o tronco sem puxar pelo pescoço.
cablepallof|Pallof press na polia|core|Abdômen|Ombros|Máquinas|0|Mantenha o tronco imóvel enquanto estende os braços.
bandchestpress|Supino em pé com elástico|push|Peito|Braços,Ombros|Elásticos|0|Prenda o elástico em apoio certificado e empurre sem trancos.
bandfly|Crucifixo com elástico|isolation|Peito|Ombros|Elásticos|0|Afaste as mãos à frente mantendo leve flexão dos cotovelos.
bandlat|Puxada alta com elástico|pull|Costas|Braços|Elásticos|0|Fixe o elástico acima da cabeça em âncora segura.
bandpullapart|Abertura de elástico (pull-apart)|pull|Costas|Ombros|Elásticos|0|Abra o elástico à frente com ombros baixos.
bandfacepull|Face pull com elástico|pull|Ombros|Costas,Braços|Elásticos|0|Puxe em direção ao rosto e confira a segurança da fixação.
bandreversefly|Crucifixo invertido com elástico|pull|Ombros|Costas|Elásticos|0|Afaste as mãos lentamente mantendo cotovelos suaves.
bandexternal|Rotação externa de ombros com elástico|isolation|Ombros||Elásticos|0|Mantenha cotovelo junto ao corpo e use pouca resistência.
bandlateral|Elevação lateral com elástico|isolation|Ombros||Elásticos|0|Eleve até amplitude confortável sem inclinar o tronco.
bandfront|Elevação frontal com elástico|isolation|Ombros||Elásticos|0|Mantenha tronco estável e controle o retorno.
bandhammer|Rosca martelo com elástico|isolation|Braços||Elásticos|0|Mantenha pegada neutra e não balance o corpo.
bandtricepsoverhead|Extensão de tríceps acima da cabeça com elástico|isolation|Braços||Elásticos|0|Mantenha cotovelos estáveis e ajuste a tensão.
bandsquat|Agachamento com elástico|squat|Pernas|Glúteos|Elásticos|0|Posicione a resistência sem prender articulações.
bandgoodmorning|Bom dia com elástico|hinge|Pernas|Glúteos|Elásticos|0|Flexione o quadril e preserve a posição neutra da coluna.
banddeadlift|Levantamento terra com elástico|hinge|Pernas|Glúteos|Elásticos|0|Ancore o elástico sob os pés com segurança.
bandglutekick|Extensão de quadril com elástico|hinge|Glúteos|Pernas|Elásticos|0|Estenda a perna sem compensação lombar.
bandmonsterwalk|Caminhada lateral com mini band|lunge|Glúteos|Pernas|Elásticos|0|Dê passos curtos mantendo tensão constante.
bandpallof|Pallof press com elástico|core|Abdômen|Ombros|Elásticos|0|Resista à rotação e mantenha as costelas estáveis.
bandpallofhold|Pallof isométrico com elástico|core|Abdômen|Ombros|Elásticos|0|Segure os braços estendidos sem girar o tronco.
bandwoodchop|Rotação diagonal com elástico|core|Abdômen|Ombros|Elásticos|1|Gire com quadris e tronco em amplitude confortável.
kbgoblet|Agachamento goblet com kettlebell|squat|Pernas|Glúteos|Kettlebell|0|Segure o peso junto ao peito e controle o agachamento.
kbfrontsquat|Agachamento frontal com dois kettlebells|squat|Pernas|Glúteos|Kettlebell|1|Apoie ambos os pesos em posição de rack sem forçar os punhos.
kbsumo|Levantamento terra sumô com kettlebell|hinge|Pernas|Glúteos|Kettlebell|0|Mantenha o peso próximo ao corpo e coluna neutra.
kbswing|Balanço kettlebell (swing)|hinge|Glúteos|Pernas,Costas|Kettlebell|1|O movimento nasce do quadril, não de levantar com os braços.
kbrow|Remada unilateral com kettlebell|pull|Costas|Braços|Kettlebell|0|Mantenha o tronco alinhado e evite rodar os ombros.
kboverhead|Desenvolvimento com kettlebell|verticalPush|Ombros|Braços|Kettlebell|1|Mantenha o punho neutro na posição de rack.
kbclean|Clean com kettlebell|hinge|Glúteos|Pernas,Ombros|Kettlebell|2|Aproxime o kettlebell do antebraço sem golpeá-lo.
kbsnatch|Snatch com kettlebell|verticalPush|Ombros|Pernas,Glúteos|Kettlebell|2|Progrida apenas após dominar o clean e o swing.
kbhalo|Halo com kettlebell|core|Abdômen|Ombros|Kettlebell|1|Passe o peso ao redor da cabeça mantendo tronco estável.
kbturkish|Turkish get-up|core|Abdômen|Ombros,Glúteos|Kettlebell|2|Pratique cada transição com carga leve e supervisão.
kbfarmers|Caminhada do fazendeiro com kettlebells|carry|Abdômen|Braços,Costas|Kettlebell|0|Caminhe sem inclinar o tronco nem perder a pegada.
kbsinglecarry|Caminhada unilateral com kettlebell|carry|Abdômen|Braços,Costas|Kettlebell|1|Evite inclinar o tronco na direção do peso.
    """.trimIndent().lines().filter { it.isNotBlank() }

    val extra: List<Exercise> by lazy {
        rows.map { line ->
            val p = line.split('|')
            check(p.size == 8) { "Registro de exercício incompleto: $line" }
            val equip = p[5].split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            Exercise(
                id = p[0], name = p[1], pattern = p[2], primaryMuscle = p[3],
                secondaryMuscles = p[4].split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
                required = equip, minimumLevel = p[6].toInt(), advice = p[7],
                modality = if (equip.isEmpty() || equip.all { it in setOf("Barra fixa", "Paralelas", "Suspensão/TRX", "Parede", "Step", "Banco") })
                    "Calistenia" else "Musculação"
            )
        }
    }
}
