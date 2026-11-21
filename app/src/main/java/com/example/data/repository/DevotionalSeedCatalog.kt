package com.example.data.repository

import com.example.data.model.BhajanTrack
import com.example.data.model.FestivalEntry
import com.example.data.model.KathaVideo
import com.example.data.model.TempleEntry
import com.example.data.model.VratInfo
import com.example.data.model.ZodiacSignInfo

/**
 * Traditional Hindu devotional reference content bundled for immediate devotional utility.
 * Owner-added Firestore entries seamlessly merge above these reference records.
 * Strictly NO fake news, NO fabricated API Panchang/Horoscope predictions, and NO fake payment data.
 */
object DevotionalSeedCatalog {

    const val THANK_YOU_MESSAGE_HI =
        "आपका यह सराहनीय सहयोग हमारे लिए अत्यंत मूल्यवान है। Bhakti Darshan परिवार की ओर से आपका हृदय से धन्यवाद। ईश्वर आपके जीवन में सुख, शांति और समृद्धि बनाए रखें।"

    val kathaCategories = listOf(
        "सभी",
        "शिव कथा",
        "श्रीराम कथा",
        "श्रीकृष्ण कथा",
        "हनुमान कथा",
        "माता रानी कथा",
        "विष्णु कथा",
        "अन्य धार्मिक कथाएँ"
    )

    val bhajanCategories = listOf(
        "All",
        "Shiva",
        "Krishna",
        "Ram",
        "Hanuman",
        "Mata Rani",
        "Aarti",
        "Chalisa",
        "Mantra"
    )

    val panchangCities = listOf(
        "Varanasi",
        "Ayodhya",
        "Ujjain",
        "Haridwar",
        "Prayagraj",
        "New Delhi",
        "Mumbai",
        "Jaipur",
        "Bengaluru",
        "Kolkata"
    )

    val zodiacSigns = listOf(
        ZodiacSignInfo("aries", "मेष", "Aries", "♈", "अग्नि", "मंगल"),
        ZodiacSignInfo("taurus", "वृषभ", "Taurus", "♉", "पृथ्वी", "शुक्र"),
        ZodiacSignInfo("gemini", "मिथुन", "Gemini", "♊", "वायु", "बुध"),
        ZodiacSignInfo("cancer", "कर्क", "Cancer", "♋", "जल", "चंद्रमा"),
        ZodiacSignInfo("leo", "सिंह", "Leo", "♌", "अग्नि", "सूर्य"),
        ZodiacSignInfo("virgo", "कन्या", "Virgo", "♍", "पृथ्वी", "बुध"),
        ZodiacSignInfo("libra", "तुला", "Libra", "♎", "वायु", "शुक्र"),
        ZodiacSignInfo("scorpio", "वृश्चिक", "Scorpio", "♏", "जल", "मंगल"),
        ZodiacSignInfo("sagittarius", "धनु", "Sagittarius", "♐", "अग्नि", "बृहस्पति"),
        ZodiacSignInfo("capricorn", "मकर", "Capricorn", "♑", "पृथ्वी", "शनि"),
        ZodiacSignInfo("aquarius", "कुंभ", "Aquarius", "♒", "वायु", "शनि"),
        ZodiacSignInfo("pisces", "मीन", "Pisces", "♓", "जल", "बृहस्पति")
    )

    val referenceVrats = listOf(
        VratInfo(
            id = "ekadashi",
            titleHi = "एकादशी व्रत",
            titleEn = "Ekadashi Vrat",
            frequencyHi = "शुक्ल एवं कृष्ण पक्ष की एकादशी (माह में 2 बार)",
            deityHi = "भगवान श्री हरि विष्णु",
            significanceHi = "एकादशी व्रत को समस्त व्रतों में श्रेष्ठ माना जाता है। यह व्रत आत्म-संयम, भक्ति और मानसिक शांति प्रदान करता है।",
            vidhiHi = "प्रातः स्नान कर श्री विष्णु का पंचामृत से अभिषेक करें, तुलसी दल अर्पित करें, विष्णु सहस्रनाम या 'ॐ नमो भगवते वासुदेवाय' का जप करें। द्वादशी तिथि पर पारण करें।",
            mantra = "ॐ नमो भगवते वासुदेवाय"
        ),
        VratInfo(
            id = "pradosh",
            titleHi = "प्रदोष व्रत",
            titleEn = "Pradosh Vrat",
            frequencyHi = "प्रत्येक पक्ष की त्रयोदशी तिथि (संध्याकाल पूजा)",
            deityHi = "भगवान शिव एवं माता पार्वती",
            significanceHi = "प्रदोष काल में भगवान शिव की आराधना से कष्टों का निवारण और कल्याण की प्राप्ति होती है।",
            vidhiHi = "प्रातः संकल्प लें और सूर्यास्त के समय प्रदोष काल में शिवलिंग पर जल, बेलपत्र, अक्षत और धूप-दीप अर्पित करें।",
            mantra = "ॐ नमः शिवाय"
        ),
        VratInfo(
            id = "shivratri",
            titleHi = "मासिक शिवरात्रि",
            titleEn = "Masik Shivratri",
            frequencyHi = "कृष्ण पक्ष की चतुर्दशी तिथि",
            deityHi = "देवाधिदेव महादेव",
            significanceHi = "चतुर्दशी की रात्रि शिव उपासना, ध्यान और जागरण के लिए अत्यंत पावन मानी जाती है।",
            vidhiHi = "निशीथ काल में भगवान शिव का रुद्राभिषेक या जलाभिषेक कर महामृत्युंजय एवं पंचाक्षर मंत्र का स्मरण करें।",
            mantra = "ॐ त्र्यम्बकं यजामहे सुगन्धिं पुष्टिवर्धनम्"
        ),
        VratInfo(
            id = "chaturthi",
            titleHi = "संकष्टी एवं विनायक चतुर्थी",
            titleEn = "Sankashti & Vinayak Chaturthi",
            frequencyHi = "प्रत्येक मास की कृष्ण एवं शुक्ल चतुर्थी",
            deityHi = "विघ्नहर्ता श्री गणेश जी",
            significanceHi = "संकटों के नाश, बुद्धि-विवेक और शुभ कार्यों में सफलता के लिए गणेश जी का चतुर्थी व्रत किया जाता है।",
            vidhiHi = "श्री गणेश को दूर्वा, सिंदूर और मोदक अर्पित करें। संकष्टी चतुर्थी पर चंद्रोदय के दर्शन व अर्घ्य के बाद पारण करें।",
            mantra = "ॐ गं गणपतये नमः"
        ),
        VratInfo(
            id = "purnima",
            titleHi = "पूर्णिमा व्रत",
            titleEn = "Purnima Vrat",
            frequencyHi = "शुक्ल पक्ष की पूर्णिमा तिथि",
            deityHi = "श्री सत्यनारायण भगवान एवं चंद्रदेव",
            significanceHi = "पूर्णिमा पर श्री सत्यनारायण कथा श्रवण, दान और पवित्र स्नान से घर में सुख-समृद्धि का वास होता है।",
            vidhiHi = "श्री सत्यनारायण पूजा करें, पंचामृत व चूरमा प्रसाद अर्पित करें और सायंकाल चंद्रदेव को अर्घ्य दें।",
            mantra = "ॐ नमो नारायणाय"
        ),
        VratInfo(
            id = "amavasya",
            titleHi = "अमावस्या (पितृ तर्पण व ध्यान)",
            titleEn = "Amavasya",
            frequencyHi = "कृष्ण पक्ष की अमावस्या तिथि",
            deityHi = "पितृ देव एवं श्री हरि",
            significanceHi = "अमावस्या पर पितरों के निमित्त तर्पण, अन्न-वस्त्र दान तथा पीपल के वृक्ष पर दीपदान का विशेष महत्व है।",
            vidhiHi = "प्रातः पवित्र स्नान के पश्चात सूर्य को अर्घ्य दें, पितरों का स्मरण करें और यथाशक्ति जरूरतमंदों को दान करें।",
            mantra = "ॐ पितृ देवताभ्यो नमः"
        )
    )

    val referenceTemples = listOf(
        TempleEntry(
            id = "ref_kashi_vishwanath",
            name = "श्री काशी विश्वनाथ मंदिर",
            location = "वाराणसी, उत्तर प्रदेश",
            deity = "भगवान श्री विश्वेश्वर (शिव ज्योतिर्लिंग)",
            history = "द्वादश ज्योतिर्लिंगों में प्रमुख श्री काशी विश्वनाथ धाम पतितपावनी माँ गंगा के तट पर स्थित है। सनातन परंपरा में काशी को मोक्षदायिनी नगरी कहा गया है।",
            darshanTimings = "मंगला आरती: 03:00 AM • सामान्य दर्शन: 04:00 AM – 11:00 PM • शयन आरती: 10:30 PM (मंदिर ट्रस्ट के अनुसार परिवर्तनशील)",
            published = true
        ),
        TempleEntry(
            id = "ref_ram_janmabhoomi",
            name = "श्री राम जन्मभूमि मंदिर",
            location = "अयोध्या धाम, उत्तर प्रदेश",
            deity = "प्रभु श्री रामलला",
            history = "पावन सरयू नदी के तट पर स्थित श्री अयोध्या धाम मर्यादा पुरुषोत्तम भगवान श्रीराम की जन्मस्थली है। नागर शैली में निर्मित भव्य मंदिर भक्तों की आस्था का प्रमुख केंद्र है।",
            darshanTimings = "प्रातः 07:00 AM – रात्रि 09:30 PM (आरती हेतु पूर्व पास व्यवस्था श्री राम जन्मभूमि तीर्थ क्षेत्र ट्रस्ट के अनुसार)",
            published = true
        ),
        TempleEntry(
            id = "ref_mahakaleshwar",
            name = "श्री महाकालेश्वर ज्योतिर्लिंग",
            location = "उज्जैन, मध्य प्रदेश",
            deity = "भगवान श्री महाकालेश्वर",
            history = "पवित्र क्षिप्रा नदी के तट पर स्थित श्री महाकालेश्वर एकमात्र दक्षिणमुखी ज्योतिर्लिंग हैं। यहाँ की प्रातःकालीन भस्म आरती विश्व प्रसिद्ध है।",
            darshanTimings = "भस्म आरती: 04:00 AM • सामान्य दर्शन: 06:00 AM – 10:30 PM",
            published = true
        ),
        TempleEntry(
            id = "ref_vaishno_devi",
            name = "श्री माता वैष्णो देवी भवन",
            location = "कटरा, त्रिकुटा पर्वत, जम्मू",
            deity = "माँ वैष्णो देवी (महाकाली, महालक्ष्मी, महासरस्वती)",
            history = "त्रिकुटा पर्वत की पावन गुफा में विराजमान माँ वैष्णो देवी के दर्शन के लिए प्रतिवर्ष करोड़ों श्रद्धालु 'जय माता दी' के जयकारों के साथ यात्रा करते हैं।",
            darshanTimings = "दर्शन 24 घंटे (प्रातः व सायं अटका आरती के समय सीमित विराम)",
            published = true
        )
    )

    val referenceFestivals = listOf(
        FestivalEntry(
            id = "ref_mahashivratri",
            name = "महाशिवरात्रि",
            dateLabel = "फाल्गुन कृष्ण चतुर्दशी",
            significance = "भगवान शिव और माता पार्वती के दिव्य मिलन तथा शिवतत्व के जागरण का महापर्व।",
            pujaVidhi = "प्रातः संकल्प लेकर चार प्रहर पूजा करें। शिवलिंग पर गंगाजल, दूध, दही, शहद, घी व बेलपत्र अर्पित कर रात्रि जागरण एवं 'ॐ नमः शिवाय' का जप करें।",
            mantra = "ॐ नमः शिवाय",
            kathaSummary = "पौराणिक मान्यता के अनुसार इस पावन रात्रि में भगवान शिव निराकार ज्योतिर्लिंग स्वरूप में प्रकट हुए और माता पार्वती संग परिणय सूत्र में बंधे।",
            published = true
        ),
        FestivalEntry(
            id = "ref_ram_navami",
            name = "श्री राम नवमी",
            dateLabel = "चैत्र शुक्ल नवमी",
            significance = "मर्यादा पुरुषोत्तम भगवान श्रीराम के प्राकट्य दिवस का पावन उत्सव।",
            pujaVidhi = "मध्याह्न काल में श्री राम दरबार का पूजन करें, रामचरितमानस का पाठ करें और पंजीरी व फलों का भोग अर्पित करें।",
            mantra = "श्री राम जय राम जय जय राम",
            kathaSummary = "अयोध्या में राजा दशरथ एवं माता कौशल्या के यहाँ धर्म की स्थापना हेतु भगवान विष्णु ने श्रीराम के रूप में अवतार धारण किया।",
            published = true
        ),
        FestivalEntry(
            id = "ref_janmashtami",
            name = "श्री कृष्ण जन्माष्टमी",
            dateLabel = "भाद्रपद कृष्ण अष्टमी",
            significance = "योगेश्वर भगवान श्रीकृष्ण के दिव्य जन्मोत्सव का आनंदमय पर्व।",
            pujaVidhi = "मध्यरात्रि 12 बजे लड्डू गोपाल का पंचामृत से अभिषेक कर झूला झुलाएं, माखन-मिश्री व धनिया पंजीरी का भोग लगाएं।",
            mantra = "ॐ नमो भगवते वासुदेवाय",
            kathaSummary = "भाद्रपद कृष्ण अष्टमी की मध्यरात्रि मथुरा में माता देवकी व वासुदेव के यहाँ श्रीकृष्ण प्रकट हुए और गोकुल में नंद-यशोदा के घर दिव्य लीलाएँ रचीं।",
            published = true
        ),
        FestivalEntry(
            id = "ref_navratri",
            name = "शारदीय नवरात्रि",
            dateLabel = "आश्विन शुक्ल प्रतिपदा से नवमी",
            significance = "माँ दुर्गा के नौ स्वरूपों (शैलपुत्री से सिद्धिदात्री) की शक्ति उपासना का नौ दिवसीय पर्व।",
            pujaVidhi = "कलश स्थापना कर प्रतिदिन अखंड ज्योति प्रज्वलित करें, दुर्गा सप्तशती का पाठ करें और अष्टमी/नवमी पर कन्या पूजन करें।",
            mantra = "या देवी सर्वभूतेषु शक्तिरूपेण संस्थिता",
            kathaSummary = "माँ दुर्गा ने नौ दिनों तक तप व शक्ति प्रदर्शन कर धर्म और सृष्टि की रक्षा की।",
            published = true
        ),
        FestivalEntry(
            id = "ref_diwali",
            name = "दीपावली",
            dateLabel = "कार्तिक अमावस्या",
            significance = "अंधकार पर प्रकाश और अधर्म पर धर्म की विजय तथा माँ लक्ष्मी व श्री गणेश के स्वागत का महापर्व।",
            pujaVidhi = "प्रदोष काल में श्री गणेश व महालक्ष्मी का पूजन कर मिट्टी के दीप प्रज्वलित करें और सर्वमंगल की कामना करें।",
            mantra = "ॐ श्रीं ह्रीं श्रीं कमले कमलालये प्रसीद प्रसीद",
            kathaSummary = "चौदह वर्ष के वनवास के पश्चात प्रभु श्रीराम के अयोध्या लौटने पर समस्त नगरवासियों ने दीपमालाओं से स्वागत किया।",
            published = true
        )
    )

    val referenceBhajans = listOf(
        BhajanTrack(
            id = "ref_hanuman_chalisa",
            title = "श्री हनुमान चालीसा (पाठ)",
            artist = "गोस्वामी तुलसीदास (पारंपरिक)",
            category = "Chalisa",
            audioUrl = null,
            lyrics = "श्रीगुरु चरन सरोज रज, निज मनु मुकुरु सुधारि।\nबरनउँ रघुबर बिमल जसु, जो दायकु फल चारि॥\nबुद्धिहीन तनु जानिके, सुमिरौं पवन-कुमार।\nबल बुधि बिद्या देहु मोहिं, हरहु कलेस बिकार॥\nजय हनुमान ज्ञान गुन सागर। जय कपीस तिहुँ लोक उजागर॥\nराम दूत अतुलित बल धामा। अंजनि-पुत्र पवनसुत नामा॥",
            durationLabel = "08:40",
            published = true
        ),
        BhajanTrack(
            id = "ref_shiv_panchakshar",
            title = "श्री शिव पंचाक्षर स्तोत्र",
            artist = "आदि शंकराचार्य (पारंपरिक)",
            category = "Shiva",
            audioUrl = null,
            lyrics = "नागेन्द्रहाराय त्रिलोचनाय भस्माङ्गरागाय महेश्वराय।\nनित्याय शुद्धाय दिगम्बराय तस्मै नकाराय नमः शिवाय॥\nमन्दाकिनीसलिलचन्दनचर्चिताय नन्दीश्वरप्रमथनाथमहेश्वराय।\nमन्दारपुष्पबहुपुष्पसुपूजिताय तस्मै मकाराय नमः शिवाय॥",
            durationLabel = "05:15",
            published = true
        ),
        BhajanTrack(
            id = "ref_krishna_achyutam",
            title = "अच्युतं केशवं कृष्ण दामोदरम्",
            artist = "पारंपरिक भक्ति स्तुति",
            category = "Krishna",
            audioUrl = null,
            lyrics = "अच्युतं केशवं कृष्ण दामोदरं, राम नारायणं जानकी वल्लभम्।\nकौन कहते हैं भगवान आते नहीं, तुम मीरा के जैसे बुलाते नहीं॥\nअच्युतं केशवं कृष्ण दामोदरं, राम नारायणं जानकी वल्लभम्॥",
            durationLabel = "04:50",
            published = true
        ),
        BhajanTrack(
            id = "ref_ram_stuti",
            title = "श्री रामचन्द्र कृपालु भजुमन",
            artist = "गोस्वामी तुलसीदास (पारंपरिक)",
            category = "Ram",
            audioUrl = null,
            lyrics = "श्री रामचन्द्र कृपालु भजुमन हरण भवभय दारुणम्।\nनवकंज लोचन कंज मुख कर कंज पद कंजारुणम्॥\nकन्दर्प अगणित अमित छवि नव नील नीरद सुन्दरम्।\nपटपीत मानहुँ तडित रुचि शुचि नौमि जनक सुतावरम्॥",
            durationLabel = "06:10",
            published = true
        ),
        BhajanTrack(
            id = "ref_mata_aarti",
            title = "जय अम्बे गौरी (माँ दुर्गा आरती)",
            artist = "पारंपरिक आरती",
            category = "Aarti",
            audioUrl = null,
            lyrics = "जय अम्बे गौरी, मैया जय श्यामा गौरी।\nतुमको निशदिन ध्यावत, हरि ब्रह्मा शिवरी॥\nमाँग सिन्दूर विराजत, टीको मृगमद को।\nउज्ज्वल से दोउ नैना, चन्द्रवदन नीको॥",
            durationLabel = "05:30",
            published = true
        ),
        BhajanTrack(
            id = "ref_gayatri_mantra",
            title = "महा गायत्री मंत्र स्मरण",
            artist = "वैदिक मंत्र",
            category = "Mantra",
            audioUrl = null,
            lyrics = "ॐ भूर्भुवः स्वः तत्सवितुर्वरेण्यं।\nभर्गो देवस्य धीमहि धियो यो नः प्रचोदयात्॥",
            durationLabel = "03:20",
            published = true
        )
    )

    val starterKathas = listOf(
        KathaVideo(
            id = "starter_shiv_katha",
            title = "शिव महापुराण — पंचाक्षर मंत्र महिमा (ओनर वीडियो जोड़ने हेतु तैयार)",
            description = "भगवान शिव की करुणा और 'ॐ नमः शिवाय' महामंत्र के आध्यात्मिक महत्व पर आधारित कथा। ओनर डैशबोर्ड से अपना अधिकृत YouTube या MP4 वीडियो लिंक कभी भी जोड़ें।",
            category = "शिव कथा",
            videoUrl = "https://www.youtube.com/results?search_query=Shiv+Mahapuran+Katha",
            duration = "18:30",
            speaker = "भक्ति दर्शन संग्रह",
            published = true
        ),
        KathaVideo(
            id = "starter_ram_katha",
            title = "श्रीराम कथा — मर्यादा, भक्ति और करुणा प्रसंग",
            description = "प्रभु श्रीराम के आदर्श जीवन और हनुमान जी की अनन्य भक्ति का प्रेरणादायी प्रसंग। ओनर डैशबोर्ड से असीमित नई कथाएँ अपलोड करें।",
            category = "श्रीराम कथा",
            videoUrl = "https://www.youtube.com/results?search_query=Shri+Ram+Katha+Hindi",
            duration = "22:15",
            speaker = "भक्ति दर्शन संग्रह",
            published = true
        )
    )
}
