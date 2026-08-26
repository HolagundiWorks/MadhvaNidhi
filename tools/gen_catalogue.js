const fs = require("fs");
const path = require("path");

const SRC = "https://madhwafestivals.com/";
const STOTRA = "https://madhwafestivals.com/2017/04/06/sthothra-compilation-on-madhwa-yatigalu/";
const TAG = "https://madhwafestivals.com/tag/sthothra/";
const NAMA = "https://madhwafestivals.com/tag/index/";
const DASA = "https://madhwafestivals.com/2017/04/15/index-dasara-padagalu-lyrics/";

function I(id, kn, sa, en, extra = {}) {
  return { id, kn, sa, en, ...extra };
}

const data = {
  source: "Madhwa Festivals master catalogue",
  site: SRC,
  groups: [
    {
      id: "stotras",
      kn: "ಸ್ತೋತ್ರಗಳು",
      sa: "स्तोत्राणि",
      en: "Stotras",
      items: [
        I("nakha_stuti", "ಶ್ರೀ ನರಸಿಂಹ ನಖ ಸ್ತುತಿ", "श्रीनृसिंह नखस्तुतिः", "Sri Narasimha Nakha Stuti", { type: "stotra", deity: "Narasimha", author: "ಮಧ್ವಾಚಾರ್ಯರು", file: "nakha_stuti.txt", script: "sa", url: STOTRA }),
        I("kanduka_stuti", "ಕಂದುಕ ಸ್ತುತಿ", "कन्दुकस्तुतिः", "Kanduka Stuti", { type: "stotra", deity: "Vayu", url: STOTRA }),
        I("dvadasha_stotra", "ದ್ವಾದಶ ಸ್ತೋತ್ರ", "द्वादशस्तोत्रम्", "Dwadasa Stotra", { type: "stotra", deity: "Hari", author: "ಮಧ್ವಾಚಾರ್ಯರು", file: "dvadasha_stotra.txt", script: "sa", url: STOTRA }),
        I("avatara_traya", "ಅವತಾರ ತ್ರಯ ಸ್ತೋತ್ರ", "अवतारत्रयस्तोत्रम्", "Avatara Traya Stotra", { type: "stotra", deity: "Vayu", url: STOTRA }),
        I("madhwashtakam", "ಶ್ರೀ ಮಧ್ವಾಷ್ಟಕಂ", "श्रीमध्वाष्टकम्", "Sri Madhwashtakam", { type: "stotra", deity: "Madhwacharya", url: STOTRA }),
        I("vayu_stuti", "ವಾಯು ಸ್ತುತಿ", "वायुस्तुतिः", "Vayu Stuti", { type: "stotra", deity: "Vayu", author: "ತ್ರಿವಿಕ್ರಮ ಪಂಡಿತಾಚಾರ್ಯರು", file: "hari_vayu_stuti.txt", script: "sa", url: STOTRA }),
        I("laghu_vayu_stuti", "ಲಘು ವಾಯು ಸ್ತುತಿ", "लघुवायुस्तुतिः", "Laghu Vayu Stuti", { type: "stotra", deity: "Vayu", url: STOTRA }),
        I("laghu_taratamya", "ಲಘು ತಾರತಮ್ಯ", "लघुतारतम्यम्", "Laghu Taratamya", { type: "stotra", url: STOTRA }),
        I("anu_vayu_stuti", "ಅಣು ವಾಯು ಸ್ತುತಿ", "अणुवायुस्तुतिः", "Anu Vayu Stuti", { type: "stotra", deity: "Vayu", url: STOTRA }),
        I("taratamya_kalyani", "ತಾರತಮ್ಯ ಸ್ತೋತ್ರ — ಕಲ್ಯಾಣಿ ದೇವಿ", "तारतम्यस्तोत्रम्", "Taratamya Stotram — Kalyani Devi", { type: "stotra", author: "ಕಲ್ಯಾಣಿ ದೇವಿ", url: STOTRA }),
        I("yantrodharaka", "ಯಂತ್ರೋದ್ಧಾರಕ ಹನುಮಾನ್ ಸ್ತೋತ್ರಂ", "यन्त्रोद्धारकहनुमत्स्तोत्रम्", "Yantrodharaka Hanuman Stotram", { type: "stotra", deity: "Hanuma", author: "ವ್ಯಾಸರಾಜರು", file: "yantrodharaka.txt", script: "sa", url: STOTRA }),
        I("grantha_malika", "ಗ್ರಂಥ ಮಾಲಿಕಾ ಸ್ತೋತ್ರಂ", "ग्रन्थमालिकास्तोत्रम्", "Grantha Malika Stotram", { type: "stotra", url: STOTRA }),
        I("srinivasa_stotram", "ಶ್ರೀನಿವಾಸ ಸ್ತೋತ್ರಂ", "श्रीनिवासस्तोत्रम्", "Srinivasa Stotram", { type: "stotra", deity: "Srinivasa", file: "lib:stotra/VenkateshaStotram", script: "sa", url: STOTRA }),
        I("laghu_shiva_stuti", "ಲಘು ಶಿವ ಸ್ತುತಿ", "लघुशिवस्तुतिः", "Laghu Shiva Stuti", { type: "stotra", deity: "Shiva", url: STOTRA }),
        I("sripadaraja_pancha", "ಶ್ರೀಪಾದರಾಜ ಪಂಚರತ್ನ ಮಾಲಿಕಾ ಸ್ತೋತ್ರ", "श्रीपादराजपञ्चरत्नमालिकास्तोत्रम्", "Sripadaraja Pancha Ratna Malika Stotra", { type: "stotra", author: "ಶ್ರೀಪಾದರಾಜರು", url: STOTRA }),
        I("jayatirtha_stuti", "ಶ್ರೀ ಜಯತೀರ್ಥ ಸ್ತುತಿ", "श्रीजयतीर्थस्तुतिः", "Sri Jayatirtha Stuti", { type: "stotra", deity: "Jayatirtha", url: STOTRA }),
        I("vyasaraja_stotram", "ಶ್ರೀ ವ್ಯಾಸರಾಜ ಸ್ತೋತ್ರಂ", "श्रीव्यासराजस्तोत्रम्", "Sri Vyasaraja Stotram", { type: "stotra", deity: "Vyasaraja", url: STOTRA }),
        I("raghuttama_stotram", "ಶ್ರೀ ರಘುತ್ತಮ ಸ್ತೋತ್ರಂ", "श्रीरघुत्तमस्तोत्रम्", "Sri Raghuttama Stotram", { type: "stotra", deity: "Raghuttama", url: STOTRA }),
        I("dvadasha_1", "ದ್ವಾದಶ ಸ್ತೋತ್ರ — ಪ್ರಥಮ ಅಧ್ಯಾಯ", "द्वादशस्तोत्रम् — प्रथमाध्यायः", "Dwadasa Stotra — First Chapter", { type: "stotra", file: "dvadasha_stotra.txt", script: "sa", url: STOTRA }),
        I("dvadasha_2", "ದ್ವಾದಶ ಸ್ತೋತ್ರ — ದ್ವಿತೀಯ ಅಧ್ಯಾಯ", "द्वादशस्तोत्रम् — द्वितीयाध्यायः", "Dwadasa Stotra — Second Chapter", { type: "stotra", url: STOTRA }),
        I("dvadasha_3", "ದ್ವಾದಶ ಸ್ತೋತ್ರ — ತೃತೀಯ ಅಧ್ಯಾಯ", "द्वादशस्तोत्रम् — तृतीयाध्यायः", "Dwadasa Stotra — Third Chapter", { type: "stotra", url: STOTRA }),
        I("dvadasha_4", "ದ್ವಾದಶ ಸ್ತೋತ್ರ — ಚತುರ್ಥ ಅಧ್ಯಾಯ", "द्वादशस्तोत्रम् — चतुर्थाध्यायः", "Dwadasa Stotra — Fourth Chapter", { type: "stotra", url: STOTRA }),
        I("dvadasha_5", "ದ್ವಾದಶ ಸ್ತೋತ್ರ — ಪಂಚಮ ಅಧ್ಯಾಯ", "द्वादशस्तोत्रम् — पञ्चमाध्यायः", "Dwadasa Stotra — Fifth Chapter", { type: "stotra", url: STOTRA }),
        I("dvadasha_6", "ದ್ವಾದಶ ಸ್ತೋತ್ರ — ಷಷ್ಠ ಅಧ್ಯಾಯ", "द्वादशस्तोत्रम् — षष्ठाध्यायः", "Dwadasa Stotra — Sixth Chapter", { type: "stotra", url: STOTRA }),
        I("dvadasha_7", "ದ್ವಾದಶ ಸ್ತೋತ್ರ — ಸಪ್ತಮ ಅಧ್ಯಾಯ", "द्वादशस्तोत्रम् — सप्तमाध्यायः", "Dwadasa Stotra — Seventh Chapter", { type: "stotra", url: STOTRA }),
        I("dvadasha_8", "ದ್ವಾದಶ ಸ್ತೋತ್ರ — ಅಷ್ಟಮ ಅಧ್ಯಾಯ", "द्वादशस्तोत्रम् — अष्टमाध्यायः", "Dwadasa Stotra — Eighth Chapter", { type: "stotra", url: STOTRA }),
        I("dvadasha_9", "ದ್ವಾದಶ ಸ್ತೋತ್ರ — ನವಮ ಅಧ್ಯಾಯ", "द्वादशस्तोत्रम् — नवमाध्यायः", "Dwadasa Stotra — Ninth Chapter", { type: "stotra", url: STOTRA }),
        I("mangalashtakam", "ಮಂಗಲಾಷ್ಟಕಂ", "मङ्गलाष्टकम्", "Mangalashtakam", { type: "stotra", url: TAG }),
        I("ramaa_stothram", "ರಮಾ ಸ್ತೋತ್ರಂ", "रमा स्तोत्रम्", "Ramaa Stothram", { type: "stotra", deity: "Lakshmi", url: TAG }),
        I("kaveri_stotra", "ಕಾವೇರಿ ಸ್ತೋತ್ರ", "कावेरी स्तोत्रम्", "Kaveri Stotra", { type: "stotra", deity: "Kaveri", file: "lib:stotra/KaveriPrarthana", script: "sa", url: TAG }),
        I("raghavendra_stotra", "ರಾಘವೇಂದ್ರ ಸ್ತೋತ್ರ", "राघवेन्द्रस्तोत्रम्", "Raghavendra Stotra", { type: "stotra", deity: "Raghavendra", file: "stotra_raghavendra.txt", script: "sa", url: STOTRA }),
        I("raghavendra_mangala", "ರಾಘವೇಂದ್ರ ಮಂಗಳ", "राघवेन्द्र मङ्गलम्", "Raghavendra Mangala", { type: "stotra", deity: "Raghavendra", file: "stotra_raghavendra_mangala.txt", script: "sa", url: STOTRA }),
        I("dashavatara_stotra", "ದಶಾವತಾರ ಸ್ತೋತ್ರ", "दशावतारस्तोत्रम्", "Dashavatara Stotra", { type: "stotra", deity: "Hari", file: "stotra_dashavatara.txt", script: "sa", url: STOTRA })
      ]
    },
    {
      id: "mantras",
      kn: "ಮಂತ್ರಗಳು",
      sa: "मन्त्राः",
      en: "Mantras",
      items: [
        I("nrisimha_mahamantra", "ಶ್ರೀ ನರಸಿಂಹ ಮಹಾಮಂತ್ರ", "श्रीनृसिंहमहामन्त्रः", "Sri Nrisimha Maha-mantra", { type: "mantra", deity: "Narasimha", file: "nrisimha_mantra.txt", script: "sa", url: SRC }),
        I("prahlada_nrisimha", "ಪ್ರಹ್ಲಾದ ವಿರಚಿತ ನರಸಿಂಹ ಸ್ತೋತ್ರಂ", "प्रह्लादविरचितनृसिंहस्तोत्रम्", "Prahlada's Narasimha Stotram", { type: "stotra", deity: "Narasimha", file: "lib:stotra/LakshmiNrisimhaKarunarasaStotram", script: "sa", url: SRC }),
        I("nrisimha_nakha", "ನರಸಿಂಹ ನಖ ಸ್ತುತಿ", "नृसिंहनखस्तुतिः", "Narasimha Nakha Stuti", { type: "stotra", deity: "Narasimha", file: "nakha_stuti.txt", script: "sa", url: SRC }),
        I("nrisimha_stuti_np", "ನರಸಿಂಹ ಸ್ತುತಿ — ನಾರಾಯಣ ಪಂಡಿತಾಚಾರ್ಯರು", "नृसिंहस्तुतिः", "Narasimha Stuti — Narayana Panditacharya", { type: "stotra", deity: "Narasimha", author: "ನಾರಾಯಣ ಪಂಡಿತಾಚಾರ್ಯರು", url: SRC }),
        I("nrusimha_ashtakam", "ಶ್ರೀ ನೃಸಿಂಹ ಅಷ್ಟಕಂ", "श्रीनृसिंहाष्टकम्", "Sri Nrusimha Ashtakam", { type: "stotra", deity: "Narasimha", url: SRC }),
        I("nrusimha_avatara", "ಶ್ರೀ ನರಸಿಂಹ ಅವತಾರ ಸ್ತೋತ್ರಂ", "श्रीनृसिंहावतारस्तोत्रम्", "Sri Nrusimha Avatara Stotram", { type: "stotra", deity: "Narasimha", url: SRC }),
        I("narahariashtakam", "ನರಹರ್ಯಷ್ಟಕಂ", "नरहर्यष्टकम्", "Narahariashtakam", { type: "stotra", deity: "Narasimha", url: SRC }),
        I("nrisimha_sanaischara", "ನರಸಿಂಹ ಸ್ತುತಿ — ಶನೈಶ್ಚರ", "नृसिंहस्तुतिः", "Narasimha Stuthi by Sanaischara", { type: "stotra", deity: "Narasimha", url: SRC }),
        I("nrisimha_dwadasha", "ನರಸಿಂಹ ದ್ವಾದಶನಾಮ ಸ್ತೋತ್ರಂ", "नृसिंहद्वादशनामस्तोत्रम्", "Narasimha Dwadasha Nama Stotram", { type: "stotra", deity: "Narasimha", file: "nrisimha_mantra.txt", script: "sa", url: SRC }),
        I("nrisimha_prapatti", "ನರಸಿಂಹ ಪ್ರಪತ್ತಿ", "नृसिंहप्रपत्तिः", "Narasimha Prapatti", { type: "stotra", deity: "Narasimha", url: SRC }),
        I("runa_vimochana", "ಋಣ ವಿಮೋಚನ ಶ್ಲೋಕಂ", "ऋणविमोचनश्लोकः", "Runa Vimochana Slokam", { type: "stotra", deity: "Narasimha", url: SRC }),
        I("pratah_sankalpa", "ಪ್ರಾತಃ ಸಂಕಲ್ಪ", "प्रातःसङ्कल्पः", "Pratah Sankalpa", { type: "mantra", file: "pratah_sankalpa.txt", script: "sa" })
      ]
    },
    {
      id: "namavalis",
      kn: "ನಾಮಾವಳಿಗಳು",
      sa: "नामावलयः",
      en: "Namavalis",
      items: [
        I("nama_ananta", "ಅನಂತ ಪದ್ಮನಾಭ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "अनन्तपद्मनाभाष्टोत्तरशतनामावलिः", "Ananta Padmanabha Ashtothra Shata Namavali", { type: "namavali", deity: "Vishnu", file: "lib:namavali/Vishnu_divyasthaniya_108", script: "sa", url: NAMA }),
        I("nama_satyanarayana", "ಶ್ರೀ ಸತ್ಯನಾರಾಯಣ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीसत्यनारायणाष्टोत्तरशतनामावलिः", "Sri Sathya Narayana Ashtothra Shata Namavali", { type: "namavali", deity: "Satyanarayana", url: NAMA }),
        I("nama_lakshmi_nrisimha", "ಲಕ್ಷ್ಮೀ ನರಸಿಂಹ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीलक्ष्मीनृसिंहाष्टोत्तरशतनामावलिः", "Lakshmi Narasimha Ashtothra Shata Namavali", { type: "namavali", deity: "Narasimha", file: "lib:namavali/LakshmiNrisimha_108", script: "sa", url: NAMA }),
        I("nama_rama", "ರಾಮ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीरामाष्टोत्तरशतनामावलिः", "Rama Ashtothra Shata Namavali", { type: "namavali", deity: "Rama", file: "lib:namavali/Rama_108", script: "sa", url: NAMA }),
        I("nama_krishna", "ಕೃಷ್ಣ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीकृष्णाष्टोत्तरशतनामावलिः", "Krishna Ashtothra Shata Namavali", { type: "namavali", deity: "Krishna", file: "lib:namavali/Krishna_108", script: "sa", url: NAMA }),
        I("nama_venkatesha", "ಶ್ರೀ ವೆಂಕಟೇಶ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीवेङ्कटेशाष्टोत्तरशतनामावलिः", "Sri Venkatesha Ashtothra Shata Namavali", { type: "namavali", deity: "Venkatesha", file: "lib:namavali/Venkatesha_108", script: "sa", url: NAMA }),
        I("nama_dhanvantari", "ಧನ್ವಂತರಿ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीधन्वन्तर्यष्टोत्तरशतनामावलिः", "Dhanvanthri Ashtothra Shata Namavali", { type: "namavali", deity: "Dhanvantari", file: "lib:namavali/Dhanvantari_108", script: "sa", url: NAMA }),
        I("nama_lakshmi", "ಶ್ರೀ ಲಕ್ಷ್ಮೀ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीलक्ष्म्यष्टोत्तरशतनामावलिः", "Sri Lakshmi Ashtothra Shata Namavali", { type: "namavali", deity: "Lakshmi", file: "lib:namavali/Lakshmi_108", script: "sa", url: NAMA }),
        I("nama_hanuma", "ಹನುಮ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीहनुमदष्टोत्तरशतनामावलिः", "Hanuma Ashtothra Shata Namavali", { type: "namavali", deity: "Hanuma", file: "lib:namavali/Anjaneya_108", script: "sa", url: NAMA }),
        I("nama_shiva", "ಶಿವ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीशिवाष्टोत्तरशतनामावलिः", "Shiva Ashtothra Shata Namavali", { type: "namavali", deity: "Shiva", file: "lib:namavali/Shiva_108", script: "sa", url: NAMA }),
        I("nama_gowri", "ಗೌರಿ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीगौर्यष्टोत्तरशतनामावलिः", "Gowri Ashtothra Namavali", { type: "namavali", deity: "Gowri", file: "lib:stotra/GauriAshtottaraShatanamaStotram", script: "sa", url: NAMA }),
        I("nama_ganesha", "ಗಣೇಶ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीगणेशाष्टोत्तरशतनामावलिः", "Ganesha Ashtothra Shata Namavali", { type: "namavali", deity: "Ganesha", file: "lib:namavali/Ganesha_108", script: "sa", url: NAMA }),
        I("nama_subrahmanya", "ಸುಬ್ರಹ್ಮಣ್ಯ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीसुब्रह्मण्याष्टोत्तरशतनामावलिः", "Subramanya Ashtothra Namavali", { type: "namavali", deity: "Subrahmanya", file: "lib:namavali/Subrahmanya_108", script: "sa", url: NAMA }),
        I("nama_durga", "ದುರ್ಗಾ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीदुर्गाष्टोत्तरशतनामावलिः", "Durga Ashtothra Shata Namavali", { type: "namavali", deity: "Durga", file: "lib:namavali/Durga_108", script: "sa", url: NAMA }),
        I("nama_saraswati", "ಸರಸ್ವತಿ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीसरस्वत्यष्टोत्तरशतनामावलिः", "Saraswathy Ashtothra Shata Namavali", { type: "namavali", deity: "Saraswati", file: "lib:namavali/Saraswati_108", script: "sa", url: NAMA }),
        I("nama_ganga", "ಗಂಗಾ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीगङ्गाष्टोत्तरशतनामावलिः", "Ganga Ashtothra Namavali", { type: "namavali", deity: "Ganga", file: "lib:namavali/Ganga_108", script: "sa", url: NAMA }),
        I("nama_kubera", "ಕುಬೇರ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीकुबेराष्टोत्तरशतनामावलिः", "Kubera Ashtothra Shata Namavali", { type: "namavali", deity: "Kubera", file: "lib:namavali/Kubera_108", script: "sa", url: NAMA }),
        I("nama_raghavendra", "ಶ್ರೀ ರಾಘವೇಂದ್ರ ಅಷ್ಟೋತ್ತರ ಶತನಾಮಾವಳಿ", "श्रीराघवेन्द्राष्टोत्तरशतनामावलिः", "Sri Raghavendra Ashtothra Shata Namavali", { type: "namavali", deity: "Raghavendra", file: "stotra_raghavendra.txt", script: "sa", url: NAMA })
      ]
    },
    {
      id: "dasara_padagalu",
      kn: "ದಾಸರ ಪದಗಳು",
      sa: "",
      en: "Dasara Padagalu",
      items: dasara()
    },
    {
      id: "suladhigalu",
      kn: "ಸುಳಾದಿಗಳು",
      sa: "सुलादयः",
      en: "Suladhis",
      items: [
        I("suladhi_index", "ಸುಳಾದಿ ಸೂಚಿ", "", "Suladhi Index", { type: "suladhi", file: "suladhi_index.txt", script: "kn" }),
        I("suladhi_nrisimha", "ನರಸಿಂಹ ಸುಳಾದಿ", "", "Nrisimha Suladhi", { type: "suladhi", deity: "Narasimha", file: "suladhi_nrisimha.txt", script: "kn" }),
        I("suladhi_hanumanta", "ಹನುಮಂತ ಸುಳಾದಿ", "", "Hanumanta Suladhi", { type: "suladhi", deity: "Hanuma", file: "suladhi_hanumanta.txt", script: "kn" }),
        I("suladhi_vayu_traya", "ವಾಯುದೇವರ ಅವತಾರ ತ್ರಯ ಸುಳಾದಿ", "", "Vayudevara Avathara Traya Suladhi", { type: "suladhi", deity: "Mukhya Prana", file: "suladhi_vayu_traya.txt", script: "kn" }),
        I("suladhi_madhwacharya", "ಮಧ್ವಾಚಾರ್ಯ ಸುಳಾದಿ", "", "Madhwacharya Suladhi", { type: "suladhi", deity: "Madhwacharya", file: "suladhi_madhwacharya.txt", script: "kn" }),
        I("suladhi_raghavendra", "ರಾಘವೇಂದ್ರ ಸುಳಾದಿ", "", "Raghavendra Suladhi", { type: "suladhi", deity: "Raghavendra", file: "suladhi_raghavendra.txt", script: "kn" }),
        I("suladhi_durga", "ದುರ್ಗಾ ಸುಳಾದಿ", "", "Durga Suladhi", { type: "suladhi", deity: "Durga", file: "suladhi_durga.txt", script: "kn" }),
        I("suladhi_hasthodhaka", "ಹಸ್ತೋದಕ ಸುಳಾದಿ", "", "Hasthodhaka Suladhi", { type: "suladhi", file: "suladhi_hasthodhaka.txt", script: "kn" }),
        I("suladhi_vivaha", "ವಿವಾಹ ಸುಳಾದಿ", "", "Vivaha Suladhi", { type: "suladhi", file: "suladhi_vivaha.txt", script: "kn" }),
        I("suladhi_pancharatna", "ಪಂಚರತ್ನ ಸುಳಾದಿ", "", "Pancharatna Suladhi", { type: "suladhi", file: "suladhi_pancharatna.txt", script: "kn" }),
        I("suladhi_madhwa_matha", "ಮಧ್ವ ಮಠ ಸುಳಾದಿ", "", "Madhwa Matha Suladhi", { type: "suladhi", file: "suladhi_madhwa_matha.txt", script: "kn" }),
        I("annadana_suladhi", "ಅನ್ನದಾನ ಮಹಿಮ ಸುಳಾದಿ", "", "Annadana Mahima Sulaadhi", { type: "suladhi", url: DASA }),
        I("runa_vimochana_suladi", "ಋಣ ವಿಮೋಚನ ಸುಳಾದಿ", "", "Runa Vimochana Suladi", { type: "suladhi", url: DASA }),
        I("moola_rama_suladhi", "ಮೂಲ ರಾಮ ದೇವರು ಸುಳಾದಿ", "", "Moola Raama Devaru Suladhi", { type: "suladhi", deity: "Rama", url: DASA })
      ]
    },
    {
      id: "ugabhogagalu",
      kn: "ಉಗಾಭೋಗಗಳು",
      sa: "",
      en: "Ugabhogas",
      items: [
        I("ugabhoga_index", "ಉಗಾಭೋಗ ಸೂಚಿ", "", "Ugabhoga Index", { type: "ugabhoga", file: "ugabhoga_index.txt", script: "kn" }),
        I("ugabhoga_sripadaraja", "ಶ್ರೀಪಾದರಾಜ ಉಗಾಭೋಗ", "", "Sripadaraja Ugabhoga", { type: "ugabhoga", author: "ಶ್ರೀಪಾದರಾಜರು", file: "ugabhoga_sripadaraja.txt", script: "kn" }),
        I("ugabhoga_favorite", "ಉಗಾಭೋಗ — ಆಯ್ದ", "", "Favorite Ugabhogas", { type: "ugabhoga", file: "ugabhoga_favorite.txt", script: "kn" })
      ]
    },
    {
      id: "nithya_parayana",
      kn: "ನಿತ್ಯ ಪಾರಾಯಣ",
      sa: "नित्यपारायणम्",
      en: "Daily Parayana",
      items: [
        I("kesava_nama", "ಕೇಶವ ನಾಮ", "", "Kesava Nama", { type: "nithya_parayana", file: "kesava_nama.txt", script: "kn" }),
        I("anu_hks", "ಅಣು ಹರಿಕಥಾಮೃತಸಾರ", "", "Anu Harikathaamruta Saara", { type: "nithya_parayana", author: "ಜಗನ್ನಾಥದಾಸರು", file: "anu_hks.txt", script: "kn", url: DASA }),
        I("hks_index", "ಹರಿಕಥಾಮೃತಸಾರ ಸಂಧಿ ಸೂಚಿ", "", "Hari Kathamruta Saara Index", { type: "nithya_parayana", author: "ಜಗನ್ನಾಥದಾಸರು", file: "hks_index.txt", script: "kn" }),
        I("hks_vigneshwara", "ವಿಘ್ನೇಶ್ವರ ಸ್ತೋತ್ರ ಸಂಧಿ", "", "Vigneshwara Sandhi", { type: "nithya_parayana", author: "ಜಗನ್ನಾಥದಾಸರು", file: "hks_vigneshwara.txt", script: "kn" }),
        I("akshaya_patre", "ಅಕ್ಷಯ ಪಾತ್ರೆ ಹಾಡು", "", "Akshaya patre haadu", { type: "nithya_parayana", url: DASA }),
        I("krishna_parijatha", "ಕೃಷ್ಣ ಪಾರಿಜಾತ", "", "Krishna parijatha", { type: "nithya_parayana", deity: "Krishna", url: DASA })
      ]
    }
  ]
};

function dasara() {
  const first = [
    ["dasa_001", "ಅಂಧಾಂತಮಸ ಪ್ರಾಪ್ತಿ", "Andhantamasa prapti", "Ekadashi"],
    ["dasa_002", "ಆ ಮಹಿಮೆಗೆ ಮಂಗಳಾರತಿ", "Aa mahimege mangalaarathi", "Hari / Narayana"],
    ["dasa_003", "ಆ ನಮಿಪೆ ಗುರು ಸಂತತಿಗೆ ಸತತ", "Aa namipe guru santathige satata", "Guru parampara"],
    ["dasa_004", "ಆಡ ಹೋಧಲ್ಲಿ", "Aada hodhalli", "Krishna"],
    ["dasa_005", "ಆಡ ಪೋಗೋನ ಬಾರೋ", "Aada pogona baaro", "Krishna"],
    ["dasa_006", "ಆನೆ ಬಂದಿದೆ", "Aane bandhidhe", "Krishna"],
    ["dasa_007", "ಆರಂಭದಲಿ ನಮಿಪೆ", "Aarambadhali namipe", "Ganesha"],
    ["dasa_008", "ಆರತಿ ಎತ್ತಿರೆ ಕೇಶವ ನಾರಾಯಣಗೆ", "Aarathi yetthire Kesava Naarayanage", "Hari / Narayana"],
    ["dasa_009", "ಆರಿಗೆ ವಧುವಾದೆ ಅಂಬುಜಾಕ್ಷಿ", "Aarige vadhuvaade ambujaakshi", "Mahalakshmi"],
    ["dasa_010", "ಆವ ಜನ್ಮದ ಪುಣ್ಯ", "Aava janmadha punya", "Krishna"],
    ["dasa_011", "ಆವ ಕಡೆಯಿಂದ ಬಂದೆ", "Aava kadeyinda bande", "Hari / Narayana"],
    ["dasa_012", "ಆಚಾರವಿಲ್ಲದ ನಾಲಿಗೆ ನಿನ್ನ", "Acharavillada nalige ninna", "Miscellaneous"],
    ["dasa_013", "ಅಡ ಹೋಗೋನ ಬಾರೋ ರಂಗ", "Ada hogona baro ranga", "Krishna"],
    ["dasa_014", "ಅದದೆಲ್ಲ ಒಳಿತೇ ಆಯಿತು", "Adadella olite ayitu", "Miscellaneous"],
    ["dasa_015", "ಆಡಿದನೋ ರಂಗ ಅದ್ಭುತದಿಂದಲಿ", "Adidano ranga adbutadindali", "Krishna"],
    ["dasa_016", "ಅಜ್ಜನಿಗಳ ಕೂಡ ಅಧಿಕ", "Ajjanigala kuda adhika", "Miscellaneous"],
    ["dasa_017", "ಅಕಟಕಟ ಸಂಸಾರವನು", "Akatakata samsaaravanu", "Miscellaneous"],
    ["dasa_018", "ಅಕ್ಕ ಕೊಳಲನುದುತ", "Akka kolalanudutha", "Krishna"],
    ["dasa_019", "ಅಕ್ಷಯ ಪಾತ್ರೆ ಹಾಡು", "Akshaya patre haadu", "Nithya parayana"],
    ["dasa_020", "ಅಲ್ಲಿ ನೋಡಲು ರಾಮ", "Alli nodalu Raama", "Raama Devaru"],
    ["dasa_021", "ಅಲ್ಲಿದೆ ನಮ್ಮನೆ ಇಲ್ಲಿ ಬಂದೆ", "Allide nammane illi bande", "Miscellaneous"],
    ["dasa_022", "ಅಲ್ಪ ಮನವರಿಗಳ್ಪರಿಸ", "Alpa manavarigalparisa", "Sri Raghavendra Theertharu"],
    ["dasa_023", "ಅಲುವದ್ಯಾತಕೋ ರಂಗ", "Aluvadyatako ranga attaranjipa gumma", "Krishna"],
    ["dasa_024", "ಅಂಬ ನೀ ಹೂವ ಪಾಲಿಸೆ ವರ", "Amba ni huva paalise vara", "Gowri / Parvathi"],
    ["dasa_025", "ಅಂಬ ತನೆಯ ಹೇ ರಂಬ", "Amba thaneya he ramba", "Ganesha"],
    ["dasa_026", "ಅಂಬೆ ಕಾಲಿಕುತಲಿ", "Ambe kalikuthali", "Krishna"],
    ["dasa_027", "ಅಂಬಿಗ ನಾ ನಿನ್ನ ನಂಬಿದೆ", "Ambiga na ninna nambide", "Miscellaneous"],
    ["dasa_028", "ಅಮ್ಮ ಲಕುಮಿ ದೇವಿ", "Amma lakumi devi", "Mahalakshmi"],
    ["dasa_029", "ಅಮ್ಮ ನಿಮ್ಮ ಮನೆಗಳಲ್ಲಿ", "Amma nimma manegalalli", "Krishna"],
    ["dasa_030", "ಆನಂದ ಆನಂದ ಮತ್ತೆ ಪರಮಾನಂದ", "Ananda Ananda matte paramananda", "Hari / Narayana"],
    ["dasa_031", "ಆನಂದ ಮಯಗೆ", "Ananda mayage", "Hari / Narayana"],
    ["dasa_032", "ಆನೆ ಬಂತನೆ ಬಂತನೆ ಬಂತಮ್ಮ", "Ane bantane bantane bantamma", "Miscellaneous"],
    ["dasa_033", "ಅಂಗಳದೊಳು ರಾಮನಾಡಿದ", "Angaladolu raamanadidha", "Raama Devaru"],
    ["dasa_034", "ಅಂಗನಾಮಣಿ ರಂಗನಾಮಣಿ", "Anganaamani ranganamani", "Miscellaneous"],
    ["dasa_035", "ಅಂಜನೇಯ ಅಮರ ವಂದಿತ", "Anjaneya amara vandhitha", "Hanuma"],
    ["dasa_036", "ಅಂಜಿಕಿನ್ಯಾತಕಯ್ಯ ಸಜ್ಜನರಿಗೆ", "Anjikinyatakayya sajjanarige", "Hanuma"],
    ["dasa_037", "ಅನ್ನದಾನ ಮಹಿಮ ಸುಳಾದಿ", "Annadana Mahima Sulaadhi", "Miscellaneous"],
    ["dasa_038", "ಅಂತಕನ ದೂತರಿಗೆ ಕಿಂಚಿತ್ತು ದಯವಿಲ್ಲ", "Antakana dutarige kincittu dayavilla", "Miscellaneous"],
    ["dasa_039", "ಅಂತರಂಗದಲ್ಲಿ ಹರಿಯ ಕಾಣದವ", "Antarangadhalli hariya kanadava", "Hari / Narayana"],
    ["dasa_040", "ಅಣು ಹರಿಕಥಾಮೃತ ಸಾರ", "Anu Harikathaamruta Saara", "Nithya Parayana"],
    ["dasa_041", "ಅನುಭವದದುಗೆಯ ಮಾಡಿ", "Anubhavadadugeya madi", "Miscellaneous"],
    ["dasa_042", "ಅಪಮೃತ್ಯು ಪರಿಹರಿಸೋ", "Apamrutyu parihariso", "Hanuma"],
    ["dasa_043", "ಅಪರಾಧವೆಣಿಸದಲೆ ಕಾಯಬೇಕು", "Aparadhavenisadale kayabeku", "Sri Madhwacharyaru"],
    ["dasa_044", "ಅಪ್ಮೃತ್ಯು ನಿವಾರಣ ಸುಳಾದಿ", "Apmrutyu nivarana Suladi", "Miscellaneous"],
    ["dasa_045", "ಅಪ್ಪತ್ತು ಪರಿಹಾರಕ ಸುಳಾದಿ", "Appattu pariharaka Suladi", "Miscellaneous"],
    ["dasa_046", "ಆಪ್ತ ಮತಿದು ನಿಜ", "Apta matidu nija", "Hari / Narayana"],
    ["dasa_047", "ಆರತಿ ಬೆಳಗ ಬನ್ನಿರೆ", "Arathi belega bannire", "Gowri / Parvathi"],
    ["dasa_048", "ಆರತಿಯ ಹೆತ್ತಮ್ಮ", "Arathiya hethamma", "Mahalakshmi"],
    ["dasa_049", "ಆರತಿ ಬೆಳಗಿರೆ ನಾರಿಯರು ಬೇಗ", "Arati belagire naariyaru bega", "Mahalakshmi"],
    ["dasa_050", "ಆರತಿಯನೆತ್ತಿರೆ ನರರೆಲ್ಲರು", "Aratiyanettire narerellaru", "Gowri / Parvathi"]
  ];
  const later = [
    ["dasa_krishna_baaro", "ಕೃಷ್ಣ ನೀ ಬೇಗನೆ ಬಾರೋ", "Krishna nee begane baaro", "Krishna"],
    ["dasa_jagadodharana", "ಜಗದೋದ್ಧಾರಣ", "Jagadodharana", "Krishna", "jagadodharana.txt"],
    ["dasa_bhagyada", "ಭಾಗ್ಯದ ಲಕ್ಷ್ಮಿ ಬಾರಮ್ಮ", "Bhagyada Lakshmi Baramma", "Mahalakshmi", "bhagyada_lakshmi.txt"],
    ["dasa_hanuma", "ಹನುಮ ಪದಗಳು", "Hanuma Padagalu", "Hanuma", "dasara_hanuma.txt"],
    ["dasa_madhwa", "ಮಧ್ವ ಪದಗಳು", "Madhwa Padagalu", "Madhwacharyaru", "dasara_madhwa.txt"],
    ["dasa_venkata", "ವೆಂಕಟಾಚಲ ನಿಲಯಂ", "Venkataachala Nilayam", "Srinivasa"],
    ["dasa_mangalam_mukhya", "ಮಂಗಳಂ ಮಂಗಳಂ ಮುಖ್ಯಪ್ರಾಣರಾಯಗೆ", "Mangalam mangalam mukyapranarayage", "Hanuma"],
    ["dasa_tulasi", "ಮಂಗಳಂ ಶ್ರೀ ತುಳಸಿ", "Mangalam sri tulasi", "Tulasi"],
    ["dasa_vithala", "ವಿಠಲ ನಿನ್ನ ನಂಬಿದೆ ಎನ್ನ ಕಾಯೋ", "Vithala Ninna Nambide Enna Kayo", "Hari / Narayana"],
    ["dasa_yenu_kaarana", "ಏನು ಕಾರಣ ಬಾಯ ತೆರೆದಿಯೋ", "Yenu Kaarana Baaya Theredhiyo", "Narasimha"],
    ["dasa_vyasa_badari", "ವ್ಯಾಸ ಬದರಿ ನಿವಾಸ", "Vyasa Badari Nivaasa", "Sri Vedavyasaru"],
    ["dasa_index", "ದಾಸರ ಪದಗಳ ಸೂಚಿ", "Dasara Padagalu Index", "Miscellaneous", "dasara_index.txt"]
  ];
  const fileById = {
    dasa_040: "anu_hks.txt",
    dasa_019: ""
  };
  const out = first.map(([id, kn, en, deity]) =>
    I(id, kn, "", en, { type: "dasara_pada", deity, script: "kn", url: DASA, file: fileById[id] || "" })
  );
  later.forEach(([id, kn, en, deity, file]) => {
    out.push(I(id, kn, "", en, { type: "dasara_pada", deity, script: "kn", url: DASA, file: file || "" }));
  });
  return out;
}

const out = path.join(__dirname, "..", "app", "src", "main", "assets", "catalogue", "index.json");
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, JSON.stringify(data, null, 2), "utf8");
const n = data.groups.reduce((a, g) => a + g.items.length, 0);
console.log("groups", data.groups.length, "items", n, "->", out);
