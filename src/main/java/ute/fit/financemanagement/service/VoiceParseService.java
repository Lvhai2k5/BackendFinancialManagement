package ute.fit.financemanagement.service;

import ute.fit.financemanagement.dto.request.VoiceRequests;
import ute.fit.financemanagement.dto.response.VoiceResponses;

public interface VoiceParseService {

    VoiceResponses.Parsed parse(VoiceRequests.Parse request, Long personId);
}
