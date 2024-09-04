package tricatch.gotpache.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.cfg.attr.VirtualDomain;
import tricatch.gotpache.cfg.attr.VirtualLocation;
import tricatch.gotpache.pass.VirtualHosts;
import tricatch.gotpache.pass.VirtualPath;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

public class VirtualHostUtil {

    private static Logger logger = LoggerFactory.getLogger(VirtualHostUtil.class);

    public static VirtualHosts convert(List<VirtualDomain> virtualDomains, Config config) throws MalformedURLException {

        VirtualHosts virtualHosts = new VirtualHosts();

        for (int v = 0; v < virtualDomains.size(); v++) {

            VirtualDomain virtualDomain = virtualDomains.get(v);

            String domain = virtualDomain.getDomain();

            try {
                String domainIp = InetAddress.getByName(domain).getHostAddress();
                if (!config.getLocalDomainIP().equals(domainIp)){
                    logger.warn("{} is not local domain", domain);
                    continue;
                }
            } catch (UnknownHostException e) {
                logger.warn("{} is not local domain", domain);
                continue;
            }

            List<VirtualPath> virtualPathListExact = new ArrayList<>();
            List<VirtualPath> virtualPathListPattern = new ArrayList<>();

            List<VirtualLocation> virtualLocationList = virtualDomain.getLocation();
            for (int p = 0; p < virtualLocationList.size(); p++) {

                VirtualLocation virtualLocation = virtualLocationList.get(p);

                URL target = new URL(virtualLocation.getHost());
                List<String> pathList = virtualLocation.getPath();

                for (int u = 0; u < pathList.size(); u++) {

                    String path = pathList.get(u);

                    VirtualPath virtualPath = new VirtualPath();
                    virtualPath.setPath(path);
                    virtualPath.setTarget(target);

                    if( path.contains("*") ){
                        virtualPathListPattern.add(virtualPath);
                    }else{
                        virtualPathListExact.add(virtualPath);
                    }

                    List<String> addHeader = new ArrayList<>();
                    List<String> removeHeader = new ArrayList<>();

                    List<String> headers = virtualLocation.getHeader();
                    if( headers!=null && !headers.isEmpty() ){
                        for(int h=0;h<headers.size();h++){
                            String header = headers.get(h);
                            if( header.startsWith("--") ){
                                header = header.substring(2);
                                removeHeader.add(header);
                            } else {
                                addHeader.add(header);
                            }
                        }
                    }

                    virtualPath.setAddHeader(addHeader);
                    virtualPath.setRemoveHeader(removeHeader);
                }
            }

            // sort by path - reverse
            Comparator<VirtualPath> virtualPathcomparator = new Comparator<VirtualPath>() {
                @Override
                public int compare(VirtualPath o1, VirtualPath o2) {
                    return o2.getPath().compareTo(o1.getPath());
                }
            };

            Collections.sort(virtualPathListExact, virtualPathcomparator);
            Collections.sort(virtualPathListPattern, virtualPathcomparator);

            List<VirtualPath> virtualPathList = new ArrayList<>();
            virtualPathList.addAll(virtualPathListExact);
            virtualPathList.addAll(virtualPathListPattern);

            virtualHosts.put(domain, virtualPathList);
        }

        return virtualHosts;
    }
}
